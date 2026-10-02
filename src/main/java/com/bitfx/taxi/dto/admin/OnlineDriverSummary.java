package com.bitfx.taxi.dto.admin;

public record OnlineDriverSummary(
        Long driverId,
        String name,
        String phone,
        String photoUrl,
        String organizationName,
        Long taxiId,
        String taxiUnitNumber,
        String taxiPlates,
        long tripsCompletedToday,
        boolean onTrip,
        Double lat,
        Double lng,
        Double heading
) {
}
