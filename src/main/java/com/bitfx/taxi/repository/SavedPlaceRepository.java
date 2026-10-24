package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.SavedPlace;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {
    List<SavedPlace> findByPassengerIdOrderByCreatedAtAsc(Long passengerId);

    long countByPassengerId(Long passengerId);

    // Para el borrado definitivo de una cuenta (ver PassengerService.deletePermanently) - son datos
    // personales de conveniencia del pasajero, no historial de negocio, asi que se borran con la cuenta.
    void deleteByPassenger(User passenger);
}
