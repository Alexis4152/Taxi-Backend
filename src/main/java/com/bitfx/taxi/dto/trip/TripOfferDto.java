package com.bitfx.taxi.dto.trip;

import java.math.BigDecimal;

public record TripOfferDto(
        Long offerId,
        Long tripId,
        double originLat,
        double originLng,
        String originAddress,
        double destinationLat,
        double destinationLng,
        String destinationAddress,
        BigDecimal distanceKm,
        BigDecimal estimatedFare,
        String paymentMethod,
        double driverDistanceKm,
        int expiresInSeconds,
        boolean wasScheduled,
        boolean babySeat,
        boolean moreThanFourPassengers,
        boolean hasPet,
        String comments
) {
}
