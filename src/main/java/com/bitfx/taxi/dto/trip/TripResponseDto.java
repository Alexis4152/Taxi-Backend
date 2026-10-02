package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.model.PaymentMethod;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TripResponseDto(
        Long id,
        Long passengerId,
        String passengerName,
        String passengerPhone,
        String passengerPhotoUrl,
        Double passengerRating,
        double originLat,
        double originLng,
        String originAddress,
        double destinationLat,
        double destinationLng,
        String destinationAddress,
        TripStatus status,
        BigDecimal distanceKm,
        BigDecimal durationMin,
        List<List<Double>> routeGeometry,
        BigDecimal estimatedFare,
        PaymentMethod paymentMethod,
        boolean paymentConfirmed,
        Long driverId,
        String driverName,
        String driverPhone,
        Double driverRating,
        String driverPhotoUrl,
        Long taxiId,
        String taxiUnitNumber,
        String taxiPlates,
        String taxiBrand,
        String taxiModel,
        LocalDateTime requestedAt,
        LocalDateTime acceptedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        LocalDateTime scheduledAt,
        String shareToken,
        BigDecimal tipAmount,
        boolean babySeat,
        boolean moreThanFourPassengers,
        boolean hasPet,
        String specialComments
) {
    public static TripResponseDto from(Trip t, List<List<Double>> routeGeometry, PassengerProfile passengerProfile) {
        return new TripResponseDto(
                t.getId(),
                t.getPassenger().getId(),
                t.getPassenger().getName(),
                t.getPassenger().getPhone(),
                passengerProfile != null ? passengerProfile.getPhotoUrl() : null,
                passengerProfile != null ? passengerProfile.getRatingAvg().doubleValue() : null,
                t.getOriginLat(),
                t.getOriginLng(),
                t.getOriginAddress(),
                t.getDestinationLat(),
                t.getDestinationLng(),
                t.getDestinationAddress(),
                t.getStatus(),
                t.getDistanceKm(),
                t.getDurationMin(),
                routeGeometry,
                t.getEstimatedFare(),
                t.getPaymentMethod(),
                t.isPaymentConfirmed(),
                t.getDriver() != null ? t.getDriver().getId() : null,
                t.getDriver() != null ? t.getDriver().getUser().getName() : null,
                t.getDriver() != null ? t.getDriver().getUser().getPhone() : null,
                t.getDriver() != null ? t.getDriver().getRatingAvg().doubleValue() : null,
                t.getDriver() != null ? t.getDriver().getPhotoUrl() : null,
                t.getTaxi() != null ? t.getTaxi().getId() : null,
                t.getTaxi() != null ? t.getTaxi().getUnitNumber() : null,
                t.getTaxi() != null ? t.getTaxi().getPlates() : null,
                t.getTaxi() != null ? t.getTaxi().getBrand() : null,
                t.getTaxi() != null ? t.getTaxi().getModel() : null,
                t.getRequestedAt(),
                t.getAcceptedAt(),
                t.getStartedAt(),
                t.getCompletedAt(),
                t.getCancelledAt(),
                t.getCancelReason(),
                t.getScheduledAt(),
                t.getShareToken(),
                t.getTipAmount(),
                t.isBabySeat(),
                t.isMoreThanFourPassengers(),
                t.isHasPet(),
                t.getSpecialComments()
        );
    }
}
