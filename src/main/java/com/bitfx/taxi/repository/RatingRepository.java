package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.Rating;
import com.bitfx.taxi.model.RatingDirection;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByFromUser_IdOrderByCreatedAtDesc(Long userId);

    // Al anonimizar una cuenta: se conserva la calificacion numerica (afecta el promedio del otro)
    // pero se borra el comentario libre que escribio.
    @Modifying
    @Query("update Rating r set r.comment = null where r.fromUser = :user")
    int clearCommentsFrom(@Param("user") User user);

    boolean existsByTripAndDirection(Trip trip, RatingDirection direction);
    Optional<Rating> findByTripAndDirection(Trip trip, RatingDirection direction);
    List<Rating> findByToUser_IdOrderByCreatedAtDesc(Long userId);
    List<Rating> findByTripInAndDirection(List<Trip> trips, RatingDirection direction);
}
