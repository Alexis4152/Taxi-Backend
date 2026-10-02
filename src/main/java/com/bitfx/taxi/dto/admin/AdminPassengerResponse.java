package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.PassengerProfile;

import java.time.LocalDateTime;

public record AdminPassengerResponse(
        Long passengerId,
        String name,
        String phone,
        String email,
        String photoUrl,
        double ratingAvg,
        int ratingCount,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminPassengerResponse from(PassengerProfile p) {
        return new AdminPassengerResponse(
                p.getId(),
                p.getUser().getName(),
                p.getUser().getPhone(),
                p.getUser().getEmail(),
                p.getPhotoUrl(),
                p.getRatingAvg().doubleValue(),
                p.getRatingCount(),
                p.getUser().isActive(),
                p.getUser().getCreatedAt()
        );
    }
}
