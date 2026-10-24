package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.PasswordResetToken;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    // Para el borrado definitivo de una cuenta (ver DriverService/PassengerService.deletePermanently).
    void deleteByUser(User user);
}
