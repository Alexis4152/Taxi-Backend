package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.TaxiChangeRequestDto;
import com.bitfx.taxi.dto.admin.TaxiChangeRequestResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.TaxiChangeRequestRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Un operador ya no cambia su propio taxi al vuelo: pide el cambio, un admin lo aprueba o rechaza,
 * y se le avisa por correo en ambos casos. Evita que cualquiera reclame el taxi de otro sin que
 * nadie lo revise.
 */
@Service
@RequiredArgsConstructor
public class TaxiChangeRequestService {

    private final TaxiChangeRequestRepository taxiChangeRequestRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final TaxiRepository taxiRepository;
    private final EmailService emailService;

    @Transactional
    public TaxiChangeRequestResponse submit(User driverUser, TaxiChangeRequestDto req) {
        DriverProfile driver = driverProfileRepository.findByUser_Id(driverUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));

        TaxiChangeRequest request = TaxiChangeRequest.builder()
                .driver(driver)
                .requestedUnitNumber(req.unitNumber())
                .requestedPlates(req.plates().trim().toUpperCase())
                .requestedBrand(req.brand())
                .requestedModel(req.model())
                .reason(req.reason())
                .status(TaxiChangeRequestStatus.PENDING)
                .build();
        request = taxiChangeRequestRepository.save(request);

        emailService.sendTaxiChangeRequestReceivedEmail(driverUser.getEmail(), driverUser.getName(), request.getRequestedPlates());
        return TaxiChangeRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public List<TaxiChangeRequestResponse> listForDriver(User driverUser) {
        DriverProfile driver = driverProfileRepository.findByUser_Id(driverUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        return taxiChangeRequestRepository.findByDriverOrderByCreatedAtDesc(driver).stream()
                .map(TaxiChangeRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxiChangeRequestResponse> listPending(Organization scopedOrg) {
        return taxiChangeRequestRepository.findByStatusOrderByCreatedAtAsc(TaxiChangeRequestStatus.PENDING).stream()
                .filter(r -> scopedOrg == null || r.getDriver().getOrganization().getId().equals(scopedOrg.getId()))
                .map(TaxiChangeRequestResponse::from)
                .toList();
    }

    @Transactional
    public TaxiChangeRequestResponse resolve(Long requestId, boolean approve, User actingAdmin, Organization scopedOrg) {
        TaxiChangeRequest request = taxiChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (scopedOrg != null && !request.getDriver().getOrganization().getId().equals(scopedOrg.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El operador no pertenece a tu organizacion");
        }
        if (request.getStatus() != TaxiChangeRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta solicitud ya fue resuelta");
        }

        DriverProfile driver = request.getDriver();
        String requestedUnitNumber = request.getRequestedUnitNumber();
        String requestedPlates = request.getRequestedPlates();
        String requestedBrand = request.getRequestedBrand();
        String requestedModel = request.getRequestedModel();
        if (approve) {
            Taxi taxi = taxiRepository.findByPlates(requestedPlates).orElseGet(() -> taxiRepository.save(
                    Taxi.builder()
                            .organization(driver.getOrganization())
                            .unitNumber(requestedUnitNumber)
                            .plates(requestedPlates)
                            .brand(requestedBrand)
                            .model(requestedModel)
                            .active(true)
                            .build()));
            driver.setTaxi(taxi);
            driverProfileRepository.save(driver);
        }

        request.setStatus(approve ? TaxiChangeRequestStatus.APPROVED : TaxiChangeRequestStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(actingAdmin);
        final TaxiChangeRequest savedRequest = taxiChangeRequestRepository.save(request);

        emailService.sendTaxiChangeRequestResolvedEmail(
                driver.getUser().getEmail(), driver.getUser().getName(), approve, requestedPlates);

        return TaxiChangeRequestResponse.from(savedRequest);
    }
}
