package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByIndependentPoolTrue();
}
