package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Rating;
import com.bitfx.taxi.model.RatingDirection;
import com.bitfx.taxi.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByTripAndDirection(Trip trip, RatingDirection direction);
    Optional<Rating> findByTripAndDirection(Trip trip, RatingDirection direction);
    List<Rating> findByToUser_IdOrderByCreatedAtDesc(Long userId);
}
