package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.Shift;
import com.bitfx.taxi.model.ShiftTemplate;
import com.bitfx.taxi.model.Taxi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByDriverAndRevokedFalse(DriverProfile driver);
    List<Shift> findByTaxiAndRevokedFalse(Taxi taxi);
    List<Shift> findByTaxiOrderByStartAtDesc(Taxi taxi);
    List<Shift> findByDriverOrderByStartAtDesc(DriverProfile driver);
    boolean existsByShiftTemplate(ShiftTemplate shiftTemplate);
}
