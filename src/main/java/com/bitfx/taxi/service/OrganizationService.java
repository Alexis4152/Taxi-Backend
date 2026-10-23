package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.OrganizationRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public List<Organization> listAll() {
        return organizationRepository.findAll();
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }

    @Transactional
    public Organization create(OrganizationRequest req) {
        Organization org = Organization.builder()
                .name(req.name())
                .contactPhone(req.contactPhone())
                .active(true)
                .build();
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization update(Long id, OrganizationRequest req) {
        Organization org = getById(id);
        org.setName(req.name());
        org.setContactPhone(req.contactPhone());
        return organizationRepository.save(org);
    }

    @Transactional
    public Organization updateLogo(Long id, String logoUrl) {
        Organization org = getById(id);
        org.setLogoUrl(logoUrl);
        return organizationRepository.save(org);
    }

    @Transactional
    public void setActive(Long id, boolean active, String reason) {
        Organization org = getById(id);
        org.setActive(active);
        // El motivo solo tiene sentido mientras esta dada de baja - se borra solo al reactivarla.
        org.setDeactivationReason(active ? null : reason);
        organizationRepository.save(org);
    }

    // Primer paso de "eliminar": bloquea la organizacion y la marca en revision, sin borrar nada
    // todavia (le da tiempo al admin de verificar antes de un borrado definitivo e irreversible).
    @Transactional
    public void markPendingDeletion(Long id, String reason) {
        Organization org = getById(id);
        requireNotIndependentPool(org);
        org.setActive(false);
        org.setPendingDeletion(true);
        org.setDeactivationReason(reason);
        organizationRepository.save(org);
    }

    @Transactional
    public void cancelPendingDeletion(Long id) {
        Organization org = getById(id);
        org.setPendingDeletion(false);
        organizationRepository.save(org);
    }

    // Borrado definitivo - solo permitido si ya paso por markPendingDeletion. Si tiene operadores,
    // taxis u otros registros ligados, la base de datos rechaza el borrado (no se hace cascada
    // manual aqui para no perder historial por error) y se lo reportamos al admin.
    @Transactional
    public void deletePermanently(Long id) {
        Organization org = getById(id);
        requireNotIndependentPool(org);
        if (!org.isPendingDeletion()) {
            throw new ApiException(HttpStatus.CONFLICT, "Primero debes marcarla como pendiente de eliminacion");
        }
        try {
            organizationRepository.delete(org);
            organizationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la organizacion tiene operadores, taxis u otros registros asociados. Permanece bloqueada como pendiente de eliminacion.");
        }
    }

    private void requireNotIndependentPool(Organization org) {
        if (org.isIndependentPool()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede bloquear ni eliminar el pool de operadores independientes");
        }
    }

    /**
     * La unica organizacion especial para operadores/taxis que no pertenecen a una organizacion
     * contratante (independientes); se crea sola la primera vez que hace falta, sin depender de un
     * seed que pudiera estar deshabilitado en produccion.
     */
    @Transactional
    public Organization resolveIndependentOrganization() {
        return organizationRepository.findByIndependentPoolTrue().orElseGet(() -> organizationRepository.save(
                Organization.builder()
                        .name("Operadores Independientes")
                        .active(true)
                        .independentPool(true)
                        .build()));
    }
}
