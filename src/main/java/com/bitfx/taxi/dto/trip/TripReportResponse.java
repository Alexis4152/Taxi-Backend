package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.TripReportCategory;
import com.bitfx.taxi.model.TripReportStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripReportResponse(
        Long id,
        Long tripId,
        String reporterName,
        String reporterRole,
        TripReportCategory category,
        String description,
        TripReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolvedByName,
        String resolutionComment,
        TripInfo trip,
        PersonDetail passenger,
        PersonDetail driver
) {
    public record TripInfo(
            String originAddress,
            String destinationAddress,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            BigDecimal estimatedFare,
            String paymentMethod,
            String status
    ) {
    }

    public record PersonDetail(
            Long userId,
            String name,
            String phone,
            String email,
            String photoUrl,
            BigDecimal ratingAvg,
            Integer ratingCount,
            String taxiUnitNumber,
            String taxiPlates
    ) {
    }
}
