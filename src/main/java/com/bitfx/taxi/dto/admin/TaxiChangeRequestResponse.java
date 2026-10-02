package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.TaxiChangeRequest;
import com.bitfx.taxi.model.TaxiChangeRequestStatus;

import java.time.LocalDateTime;

public record TaxiChangeRequestResponse(
        Long id,
        Long driverId,
        String driverName,
        String requestedUnitNumber,
        String requestedPlates,
        String requestedBrand,
        String requestedModel,
        String reason,
        TaxiChangeRequestStatus status,
        LocalDateTime createdAt
) {
    public static TaxiChangeRequestResponse from(TaxiChangeRequest r) {
        return new TaxiChangeRequestResponse(
                r.getId(),
                r.getDriver().getId(),
                r.getDriver().getUser().getName(),
                r.getRequestedUnitNumber(),
                r.getRequestedPlates(),
                r.getRequestedBrand(),
                r.getRequestedModel(),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
