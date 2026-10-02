package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    // El correo no es unico entre cuentas (una misma persona puede tener cuenta de operador y de
    // pasajero con telefonos distintos pero el mismo correo), asi que puede haber mas de un match.
    java.util.List<User> findAllByEmailIgnoreCase(String email);
}
