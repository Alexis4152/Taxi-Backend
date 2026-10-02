package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.DriverLocation;
import com.bitfx.taxi.model.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    Optional<DriverLocation> findByDriver(DriverProfile driver);
    List<DriverLocation> findByOnlineTrue();
}
