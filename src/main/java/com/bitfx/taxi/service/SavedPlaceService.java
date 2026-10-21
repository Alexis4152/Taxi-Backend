package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.passenger.SavedPlaceDto;
import com.bitfx.taxi.dto.passenger.SavedPlaceResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.SavedPlace;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.SavedPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Direcciones favoritas del pasajero (ej. "Casa", "Trabajo"), para llenar origen/destino sin
 * buscar la direccion cada vez que pide un viaje.
 */
@Service
@RequiredArgsConstructor
public class SavedPlaceService {

    private static final int MAX_PLACES_PER_PASSENGER = 10;

    private final SavedPlaceRepository savedPlaceRepository;

    @Transactional(readOnly = true)
    public List<SavedPlaceResponse> list(Long passengerId) {
        return savedPlaceRepository.findByPassengerIdOrderByCreatedAtAsc(passengerId).stream()
                .map(SavedPlaceResponse::from)
                .toList();
    }

    @Transactional
    public SavedPlaceResponse create(User passenger, SavedPlaceDto req) {
        if (savedPlaceRepository.countByPassengerId(passenger.getId()) >= MAX_PLACES_PER_PASSENGER) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya alcanzaste el maximo de " + MAX_PLACES_PER_PASSENGER + " direcciones guardadas");
        }
        SavedPlace place = SavedPlace.builder()
                .passenger(passenger)
                .label(req.label())
                .address(req.address())
                .lat(req.lat())
                .lng(req.lng())
                .build();
        return SavedPlaceResponse.from(savedPlaceRepository.save(place));
    }

    @Transactional
    public void delete(Long passengerId, Long placeId) {
        SavedPlace place = savedPlaceRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Direccion no encontrada"));
        if (!place.getPassenger().getId().equals(passengerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta direccion no te pertenece");
        }
        savedPlaceRepository.delete(place);
    }
}
