package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.Taxi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxiRepository extends JpaRepository<Taxi, Long> {
    List<Taxi> findByOrganization(Organization organization);
    Optional<Taxi> findByOrganizationAndUnitNumber(Organization organization, String unitNumber);
    Optional<Taxi> findByOrganizationAndUnitNumberAndActiveTrue(Organization organization, String unitNumber);
    Optional<Taxi> findByPlates(String plates);
}
