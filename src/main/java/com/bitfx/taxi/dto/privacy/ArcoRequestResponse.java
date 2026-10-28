package com.bitfx.taxi.dto.privacy;

import com.bitfx.taxi.model.ArcoRequest;
import com.bitfx.taxi.model.ArcoRequestStatus;
import com.bitfx.taxi.model.ArcoRequestType;
import com.bitfx.taxi.model.Role;

import java.time.LocalDateTime;

public record ArcoRequestResponse(
        Long id,
        String folio,
        ArcoRequestType type,
        String description,
        ArcoRequestStatus status,
        String response,
        LocalDateTime createdAt,
        LocalDateTime dueAt,
        LocalDateTime resolvedAt,
        Long userId,
        String userName,
        String userPhone,
        String userEmail,
        Role userRole,
        boolean userAnonymized
) {
    public static ArcoRequestResponse from(ArcoRequest r) {
        var user = r.getUser();
        return new ArcoRequestResponse(
                r.getId(),
                r.getFolio(),
                r.getType(),
                r.getDescription(),
                r.getStatus(),
                r.getResponse(),
                r.getCreatedAt(),
                r.getDueAt(),
                r.getResolvedAt(),
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getAnonymizedAt() != null
        );
    }
}
