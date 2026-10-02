package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.TariffRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TariffRuleRepository extends JpaRepository<TariffRule, Long> {
    Optional<TariffRule> findByOrganization(Organization organization);
    Optional<TariffRule> findByOrganizationIsNull();
}
