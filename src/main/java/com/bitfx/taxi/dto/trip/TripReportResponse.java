package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripReport;
import com.bitfx.taxi.model.TripReportCategory;
import com.bitfx.taxi.model.TripReportStatus;

import java.time.LocalDateTime;

public record TripReportResponse(
        Long id,
        Long tripId,
        String reporterName,
        String reporterRole,
        String otherPartyName,
        TripReportCategory category,
        String description,
        TripReportStatus status,
        LocalDateTime createdAt
) {
    public static TripReportResponse from(TripReport r) {
        Trip trip = r.getTrip();
        boolean reporterIsPassenger = trip.getPassenger().getId().equals(r.getReporter().getId());
        String otherPartyName = reporterIsPassenger
                ? (trip.getDriver() != null ? trip.getDriver().getUser().getName() : null)
                : trip.getPassenger().getName();
        return new TripReportResponse(
                r.getId(),
                trip.getId(),
                r.getReporter().getName(),
                r.getReporter().getRole().name(),
                otherPartyName,
                r.getCategory(),
                r.getDescription(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
