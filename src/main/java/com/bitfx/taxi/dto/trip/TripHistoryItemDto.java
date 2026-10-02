package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Version ligera de un viaje para listas de historial (no recalcula la ruta como TripResponseDto,
 * asi que es barato de generar aunque sean muchos viajes).
 */
public record TripHistoryItemDto(
        Long id,
        String originAddress,
        String destinationAddress,
        String status,
        BigDecimal estimatedFare,
        String paymentMethod,
        String otherPartyName,
        LocalDateTime requestedAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancelledByRole
) {
    public static TripHistoryItemDto forPassenger(Trip t) {
        return new TripHistoryItemDto(
                t.getId(), t.getOriginAddress(), t.getDestinationAddress(), t.getStatus().name(),
                t.getEstimatedFare(), t.getPaymentMethod().name(),
                t.getDriver() != null ? t.getDriver().getUser().getName() : null,
                t.getRequestedAt(), t.getAcceptedAt(), t.getCompletedAt(), t.getCancelledAt(),
                t.getCancelledByRole() != null ? t.getCancelledByRole().name() : null
        );
    }

    public static TripHistoryItemDto forDriver(Trip t) {
        return new TripHistoryItemDto(
                t.getId(), t.getOriginAddress(), t.getDestinationAddress(), t.getStatus().name(),
                t.getEstimatedFare(), t.getPaymentMethod().name(),
                t.getPassenger().getName(),
                t.getRequestedAt(), t.getAcceptedAt(), t.getCompletedAt(), t.getCancelledAt(),
                t.getCancelledByRole() != null ? t.getCancelledByRole().name() : null
        );
    }
}
