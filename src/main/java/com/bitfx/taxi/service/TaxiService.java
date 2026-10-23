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
                .wheelchairAccessible(req.wheelchairAccessible())
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
        taxi.setWheelchairAccessible(req.wheelchairAccessible());
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
    public void setActive(Long taxiId, boolean active, String reason) {
        Taxi taxi = getById(taxiId);
        taxi.setActive(active);
        taxi.setDeactivationReason(active ? null : reason);
        taxiRepository.save(taxi);
    }

    // Primer paso de "eliminar": bloquea el taxi y lo marca en revision, sin borrar nada todavia
    // (le da tiempo al admin de verificar antes de un borrado definitivo e irreversible).
    @Transactional
    public void markPendingDeletion(Long taxiId, String reason) {
        Taxi taxi = getById(taxiId);
        taxi.setActive(false);
        taxi.setPendingDeletion(true);
        taxi.setDeactivationReason(reason);
        taxiRepository.save(taxi);
    }

    @Transactional
    public void cancelPendingDeletion(Long taxiId) {
        Taxi taxi = getById(taxiId);
        taxi.setPendingDeletion(false);
        taxiRepository.save(taxi);
    }

    // Borrado definitivo - solo permitido si ya paso por markPendingDeletion. Si tiene operadores
    // vinculados, viajes u otros registros, la base de datos rechaza el borrado (no se hace cascada
    // manual aqui para no perder historial por error) y se lo reportamos al admin.
    @Transactional
    public void deletePermanently(Long taxiId) {
        Taxi taxi = getById(taxiId);
        if (!taxi.isPendingDeletion()) {
            throw new ApiException(HttpStatus.CONFLICT, "Primero debes marcarlo como pendiente de eliminacion");
        }
        try {
            taxiRepository.delete(taxi);
            taxiRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el taxi tiene operadores vinculados, viajes u otros registros asociados. Permanece bloqueado como pendiente de eliminacion.");
        }
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return organizationService.resolveIndependentOrganization();
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }
}
