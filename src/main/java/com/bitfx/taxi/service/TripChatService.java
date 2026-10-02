package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.TripMessageDto;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripMessage;
import com.bitfx.taxi.model.TripStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.TripMessageRepository;
import com.bitfx.taxi.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Chat de texto libre entre pasajero y operador, disponible mientras el viaje esta aceptado o en
 * curso (desde que el operador acepta, antes de recoger al pasajero, hasta que el viaje termina).
 */
@Service
@RequiredArgsConstructor
public class TripChatService {

    private static final List<TripStatus> CHAT_ALLOWED_STATUSES = List.of(TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    private final TripMessageRepository tripMessageRepository;
    private final TripRepository tripRepository;
    private final TripNotifier tripNotifier;

    public List<TripMessageDto> listMessages(User user, Long tripId) {
        requireParticipant(user, tripId);
        return tripMessageRepository.findByTrip_IdOrderByCreatedAtAsc(tripId).stream()
                .map(TripMessageDto::from)
                .toList();
    }

    @Transactional
    public TripMessageDto sendMessage(User user, Long tripId, String body) {
        Trip trip = requireParticipant(user, tripId);
        if (!CHAT_ALLOWED_STATUSES.contains(trip.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "El chat solo esta disponible mientras el viaje esta aceptado o en curso");
        }
        TripMessage message = TripMessage.builder()
                .trip(trip)
                .senderUser(user)
                .body(body)
                .build();
        message = tripMessageRepository.save(message);

        TripMessageDto dto = TripMessageDto.from(message);
        tripNotifier.notifyNewMessage(tripId, dto);
        return dto;
    }

    private Trip requireParticipant(User user, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        boolean isPassenger = trip.getPassenger().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(user.getId());
        if (!isPassenger && !isDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No participas en este viaje");
        }
        return trip;
    }
}
