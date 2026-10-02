package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.Taxi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUser_Id(Long userId);
    List<DriverProfile> findByOrganization(Organization organization);
    List<DriverProfile> findByTaxi(Taxi taxi);
}
