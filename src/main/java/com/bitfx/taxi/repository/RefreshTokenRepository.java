package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.RefreshToken;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    // Para el borrado definitivo de una cuenta (ver DriverService/PassengerService.deletePermanently):
    // sin esto, hasta una fila ya revocada sigue bloqueando el borrado del usuario por la llave
    // foranea, aunque la cuenta nunca haya tenido viajes ni nada de historial real.
    void deleteByUser(User user);
}
