package com.bitfx.taxi.dto;

import com.bitfx.taxi.model.PassengerProfile;

public record PassengerResponse(
        Long passengerId,
        String name,
        String phone,
        String email,
        String photoUrl,
        double ratingAvg,
        int ratingCount
) {
    public static PassengerResponse from(PassengerProfile p) {
        return new PassengerResponse(
                p.getId(),
                p.getUser().getName(),
                p.getUser().getPhone(),
                p.getUser().getEmail(),
                p.getPhotoUrl(),
                p.getRatingAvg().doubleValue(),
                p.getRatingCount()
        );
    }
}
