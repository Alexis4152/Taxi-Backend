package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.TaxiChangeRequest;
import com.bitfx.taxi.model.TaxiChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxiChangeRequestRepository extends JpaRepository<TaxiChangeRequest, Long> {
    List<TaxiChangeRequest> findByStatusOrderByCreatedAtAsc(TaxiChangeRequestStatus status);
    List<TaxiChangeRequest> findByDriverOrderByCreatedAtDesc(DriverProfile driver);
}
