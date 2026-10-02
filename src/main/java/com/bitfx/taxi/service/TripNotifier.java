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
        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/offers", java.util.Map.of("offerId", offerId, "closed", true));
    }

    public void notifyNewMessage(Long tripId, TripMessageDto message) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/messages", message);
    }

    public void notifyDriverLocation(Long tripId, double lat, double lng, Double heading) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/driver-location",
                java.util.Map.of("lat", lat, "lng", lng, "heading", heading == null ? 0 : heading));
    }
}
