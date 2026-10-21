package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {
    List<SavedPlace> findByPassengerIdOrderByCreatedAtAsc(Long passengerId);

    long countByPassengerId(Long passengerId);
}
