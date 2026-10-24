package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.TrustedContact;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {
    List<TrustedContact> findByPassengerIdOrderByCreatedAtAsc(Long passengerId);

    long countByPassengerId(Long passengerId);

    // Para el borrado definitivo de una cuenta (ver PassengerService.deletePermanently).
    void deleteByPassenger(User passenger);
}
