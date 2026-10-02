package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.PassengerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, Long> {
    Optional<PassengerProfile> findByUser_Id(Long userId);
}
