package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.OfferStatus;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripOfferRepository extends JpaRepository<TripOffer, Long> {
    List<TripOffer> findByTrip(Trip trip);
    Optional<TripOffer> findByTripAndDriver(Trip trip, DriverProfile driver);
    List<TripOffer> findByDriverAndStatus(DriverProfile driver, OfferStatus status);
}
