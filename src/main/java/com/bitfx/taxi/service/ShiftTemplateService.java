package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.ShiftTemplateRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.ShiftTemplate;
import com.bitfx.taxi.repository.OrganizationRepository;
import com.bitfx.taxi.repository.ShiftRepository;
import com.bitfx.taxi.repository.ShiftTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftTemplateService {

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftRepository shiftRepository;
    private final OrganizationRepository organizationRepository;

    public List<ShiftTemplate> listForOrg(Organization org) {
        return org != null ? shiftTemplateRepository.findByOrganizationOrderByStartTime(org) : shiftTemplateRepository.findAll();
    }

    public ShiftTemplate getById(Long id, Organization scopedOrg) {
        ShiftTemplate template = shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plantilla de turno no encontrada"));
        requireSameOrg(template, scopedOrg);
        return template;
    }

    @Transactional
    public ShiftTemplate create(ShiftTemplateRequest req, Organization scopedOrg) {
        Organization org = scopedOrg != null ? scopedOrg : resolveOrganization(req.organizationId());
        ShiftTemplate template = ShiftTemplate.builder()
                .organization(org)
                .name(req.name())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .active(true)
                .build();
        try {
            return shiftTemplateRepository.save(template);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una plantilla de turno con ese nombre en esta organizacion");
        }
    }

    @Transactional
    public ShiftTemplate update(Long id, ShiftTemplateRequest req, Organization scopedOrg) {
        ShiftTemplate template = getById(id, scopedOrg);
        template.setName(req.name());
        template.setStartTime(req.startTime());
        template.setEndTime(req.endTime());
        try {
            return shiftTemplateRepository.save(template);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una plantilla de turno con ese nombre en esta organizacion");
        }
    }

    @Transactional
    public void delete(Long id, Organization scopedOrg) {
        ShiftTemplate template = getById(id, scopedOrg);
        if (shiftRepository.existsByShiftTemplate(template)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: hay turnos que usan esta plantilla. Desactivala en su lugar o termina esos turnos primero.");
        }
        shiftTemplateRepository.delete(template);
    }

    private void requireSameOrg(ShiftTemplate template, Organization scopedOrg) {
        if (scopedOrg != null && !template.getOrganization().getId().equals(scopedOrg.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta plantilla no pertenece a tu organizacion");
        }
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debes indicar la organizacion");
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }
}
