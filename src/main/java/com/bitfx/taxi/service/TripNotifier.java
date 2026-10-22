package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.TripMessageDto;
import com.bitfx.taxi.dto.trip.TripOfferDto;
import com.bitfx.taxi.dto.trip.TripResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendOfferToDriver(Long driverId, TripOfferDto offer) {
        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/offers", offer);
    }

    public void notifyTripUpdate(Long tripId, TripResponseDto trip) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId, trip);
    }

    public void notifyOfferClosed(Long driverId, Long offerId) {
        notifyOfferClosed(driverId, offerId, null);
    }

    /**
     * reason distingue por que se cierra, para que el frontend decida como reaccionar: null (lo
     * tomo otro operador, o expiro sola) se quita de la lista en silencio; "CANCELLED_BY_PASSENGER"
     * se muestra brevemente como "viaje cancelado" en vez de desaparecer de inmediato, para que el
     * operador no se quede con la duda de por que ya no puede aceptarla.
     */
    public void notifyOfferClosed(Long driverId, Long offerId, String reason) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("offerId", offerId);
        payload.put("closed", true);
        if (reason != null) payload.put("reason", reason);
        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/offers", payload);
    }

    public void notifyNewMessage(Long tripId, TripMessageDto message) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/messages", message);
    }

    public void notifyDriverLocation(Long tripId, double lat, double lng, Double heading) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/driver-location",
                java.util.Map.of("lat", lat, "lng", lng, "heading", heading == null ? 0 : heading));
    }
}
