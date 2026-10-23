package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.Taxi;

public record TaxiResponse(
        Long id,
        String unitNumber,
        String plates,
        String brand,
        String model,
        String photoUrl,
        boolean active,
        String deactivationReason,
        boolean pendingDeletion,
        Long organizationId,
        String organizationName,
        boolean wheelchairAccessible
) {
    public static TaxiResponse from(Taxi t) {
        return new TaxiResponse(
                t.getId(),
                t.getUnitNumber(),
                t.getPlates(),
                t.getBrand(),
                t.getModel(),
                t.getPhotoUrl(),
                t.isActive(),
                t.getDeactivationReason(),
                t.isPendingDeletion(),
                t.getOrganization().getId(),
                t.getOrganization().getName(),
                t.isWheelchairAccessible()
        );
    }
}
