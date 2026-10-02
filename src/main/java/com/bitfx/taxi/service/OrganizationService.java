package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.OrganizationRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
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
    public void setActive(Long id, boolean active) {
        Organization org = getById(id);
        org.setActive(active);
        organizationRepository.save(org);
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
