package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.TripMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripMessageRepository extends JpaRepository<TripMessage, Long> {
    List<TripMessage> findByTrip_IdOrderByCreatedAtAsc(Long tripId);
}
