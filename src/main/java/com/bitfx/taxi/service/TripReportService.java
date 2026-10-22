package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.ResolveTripReportRequest;
import com.bitfx.taxi.dto.trip.TripReportDto;
import com.bitfx.taxi.dto.trip.TripReportResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripReport;
import com.bitfx.taxi.model.TripReportStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.TripReportRepository;
import com.bitfx.taxi.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes de un problema en un viaje, hechos por el pasajero o el operador que lo vivieron. No se
 * resuelven solos: un admin los revisa, deja un comentario explicando la resolucion y eso se le
 * notifica por correo a ambas partes del viaje (igual que TaxiChangeRequest, pero aqui siempre
 * hacen falta un comentario y doble notificacion, ya que involucra a dos personas distintas).
 */
@Service
@RequiredArgsConstructor
public class TripReportService {

    private final TripReportRepository tripReportRepository;
    private final TripRepository tripRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final EmailService emailService;

    @Transactional
    public TripReportResponse submit(User reporter, Long tripId, TripReportDto req) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        boolean isPassenger = trip.getPassenger().getId().equals(reporter.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(reporter.getId());
        if (!isPassenger && !isDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes reportar un viaje que no es tuyo");
        }

        TripReport report = TripReport.builder()
                .trip(trip)
                .reporter(reporter)
                .category(req.category())
                .description(req.description())
                .status(TripReportStatus.PENDING)
                .build();
        return toResponse(tripReportRepository.save(report));
    }

    /**
     * status null = todos (pendientes primero los mas viejos, resueltos primero los mas
     * recientes) - ver TripReportController, que antes solo exponia los pendientes y dejaba los ya
     * resueltos sin forma de volver a consultarlos.
     */
    @Transactional(readOnly = true)
    public List<TripReportResponse> list(Organization scopedOrg, TripReportStatus status) {
        List<TripReport> reports;
        if (status == TripReportStatus.PENDING) {
            reports = tripReportRepository.findByStatusOrderByCreatedAtAsc(status);
        } else if (status == TripReportStatus.RESOLVED) {
            reports = tripReportRepository.findByStatusOrderByResolvedAtDesc(status);
        } else {
            reports = tripReportRepository.findAllByOrderByCreatedAtDesc();
        }
        return reports.stream()
                .filter(r -> scopedOrg == null
                        || (r.getTrip().getOrganization() != null && r.getTrip().getOrganization().getId().equals(scopedOrg.getId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TripReportResponse resolve(Long reportId, User actingAdmin, Organization scopedOrg, ResolveTripReportRequest req) {
        TripReport report = tripReportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        Organization tripOrg = report.getTrip().getOrganization();
        if (scopedOrg != null && (tripOrg == null || !tripOrg.getId().equals(scopedOrg.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este reporte no pertenece a tu organizacion");
        }
        if (report.getStatus() != TripReportStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Este reporte ya fue resuelto");
        }
        report.setStatus(TripReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(actingAdmin);
        report.setResolutionComment(req.comment());
        report = tripReportRepository.save(report);

        notifyBothParties(report);
        return toResponse(report);
    }

    private void notifyBothParties(TripReport report) {
        Trip trip = report.getTrip();
        emailService.sendTripReportResolvedEmail(trip.getPassenger().getEmail(), trip.getPassenger().getName(),
                trip.getId(), report.getResolutionComment());
        if (trip.getDriver() != null) {
            User driverUser = trip.getDriver().getUser();
            emailService.sendTripReportResolvedEmail(driverUser.getEmail(), driverUser.getName(),
                    trip.getId(), report.getResolutionComment());
        }
    }

    private TripReportResponse toResponse(TripReport r) {
        Trip trip = r.getTrip();

        PassengerProfile passengerProfile = passengerProfileRepository.findByUser_Id(trip.getPassenger().getId()).orElse(null);
        TripReportResponse.PersonDetail passengerDetail = new TripReportResponse.PersonDetail(
                trip.getPassenger().getId(),
                trip.getPassenger().getName(),
                trip.getPassenger().getPhone(),
                trip.getPassenger().getEmail(),
                passengerProfile != null ? passengerProfile.getPhotoUrl() : null,
                passengerProfile != null ? passengerProfile.getRatingAvg() : null,
                passengerProfile != null ? passengerProfile.getRatingCount() : null,
                null,
                null
        );

        TripReportResponse.PersonDetail driverDetail = null;
        if (trip.getDriver() != null) {
            DriverProfile driverProfile = trip.getDriver();
            User driverUser = driverProfile.getUser();
            driverDetail = new TripReportResponse.PersonDetail(
                    driverUser.getId(),
                    driverUser.getName(),
                    driverUser.getPhone(),
                    driverUser.getEmail(),
                    driverProfile.getPhotoUrl(),
                    driverProfile.getRatingAvg(),
                    driverProfile.getRatingCount(),
                    trip.getTaxi() != null ? trip.getTaxi().getUnitNumber() : null,
                    trip.getTaxi() != null ? trip.getTaxi().getPlates() : null
            );
        }

        TripReportResponse.TripInfo tripInfo = new TripReportResponse.TripInfo(
                trip.getOriginAddress(),
                trip.getDestinationAddress(),
                trip.getRequestedAt(),
                trip.getCompletedAt(),
                trip.getEstimatedFare(),
                trip.getPaymentMethod() != null ? trip.getPaymentMethod().name() : null,
                trip.getStatus().name()
        );

        return new TripReportResponse(
                r.getId(),
                trip.getId(),
                r.getReporter().getName(),
                r.getReporter().getRole().name(),
                r.getCategory(),
                r.getDescription(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getResolvedAt(),
                r.getResolvedBy() != null ? r.getResolvedBy().getName() : null,
                r.getResolutionComment(),
                tripInfo,
                passengerDetail,
                driverDetail
        );
    }
}
