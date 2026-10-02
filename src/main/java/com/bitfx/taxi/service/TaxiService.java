package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.TaxiRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.Taxi;
import com.bitfx.taxi.repository.OrganizationRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxiService {

    private final TaxiRepository taxiRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;

    public List<Taxi> listForOrg(Organization org) {
        return org != null ? taxiRepository.findByOrganization(org) : taxiRepository.findAll();
    }

    public Taxi getById(Long id) {
        return taxiRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Taxi no encontrado"));
    }

    @Transactional
    public Taxi create(TaxiRequest req, Organization scopedOrg) {
        Organization org = scopedOrg != null ? scopedOrg : resolveOrganization(req.organizationId());
        Taxi taxi = Taxi.builder()
                .organization(org)
                .unitNumber(req.unitNumber())
                .plates(req.plates().toUpperCase())
                .brand(req.brand())
                .model(req.model())
                .active(true)
                .build();
        try {
            return taxiRepository.save(taxi);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un taxi con ese numero de unidad o placas");
        }
    }

    @Transactional
    public Taxi update(Long taxiId, TaxiRequest req) {
        Taxi taxi = getById(taxiId);
        taxi.setUnitNumber(req.unitNumber());
        taxi.setPlates(req.plates().toUpperCase());
        taxi.setBrand(req.brand());
        taxi.setModel(req.model());
        try {
            return taxiRepository.save(taxi);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un taxi con ese numero de unidad o placas");
        }
    }

    @Transactional
    public Taxi updatePhoto(Long taxiId, String photoUrl) {
        Taxi taxi = getById(taxiId);
        taxi.setPhotoUrl(photoUrl);
        return taxiRepository.save(taxi);
    }

    @Transactional
    public void setActive(Long taxiId, boolean active) {
        Taxi taxi = getById(taxiId);
        taxi.setActive(active);
        taxiRepository.save(taxi);
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return organizationService.resolveIndependentOrganization();
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }
}
