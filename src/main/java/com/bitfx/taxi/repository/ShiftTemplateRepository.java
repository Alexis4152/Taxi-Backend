package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
    List<ShiftTemplate> findByOrganizationOrderByStartTime(Organization organization);
}
