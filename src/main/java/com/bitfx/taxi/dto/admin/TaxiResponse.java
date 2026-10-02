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
        Long organizationId,
        String organizationName
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
                t.getOrganization().getId(),
                t.getOrganization().getName()
        );
    }
}
