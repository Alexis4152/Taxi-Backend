package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.passenger.TrustedContactDto;
import com.bitfx.taxi.dto.passenger.TrustedContactResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.TrustedContact;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.TrustedContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contactos de confianza del pasajero, para compartirles rapido el seguimiento en vivo de un
 * viaje (ver TripService.shareToken) sin tener que buscarlos cada vez.
 */
@Service
@RequiredArgsConstructor
public class TrustedContactService {

    private static final int MAX_CONTACTS_PER_PASSENGER = 5;

    private final TrustedContactRepository trustedContactRepository;

    @Transactional(readOnly = true)
    public List<TrustedContactResponse> list(Long passengerId) {
        return trustedContactRepository.findByPassengerIdOrderByCreatedAtAsc(passengerId).stream()
                .map(TrustedContactResponse::from)
                .toList();
    }

    @Transactional
    public TrustedContactResponse create(User passenger, TrustedContactDto req) {
        if (trustedContactRepository.countByPassengerId(passenger.getId()) >= MAX_CONTACTS_PER_PASSENGER) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya alcanzaste el maximo de " + MAX_CONTACTS_PER_PASSENGER + " contactos de confianza");
        }
        TrustedContact contact = TrustedContact.builder()
                .passenger(passenger)
                .name(req.name())
                .phone(req.phone())
                .build();
        return TrustedContactResponse.from(trustedContactRepository.save(contact));
    }

    @Transactional
    public void delete(Long passengerId, Long contactId) {
        TrustedContact contact = trustedContactRepository.findById(contactId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Contacto no encontrado"));
        if (!contact.getPassenger().getId().equals(passengerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este contacto no te pertenece");
        }
        trustedContactRepository.delete(contact);
    }
}
