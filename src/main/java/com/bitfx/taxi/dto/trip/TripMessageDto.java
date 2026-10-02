package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.TripMessage;

import java.time.LocalDateTime;

public record TripMessageDto(
        Long id,
        Long tripId,
        Long senderUserId,
        String senderName,
        String senderRole,
        String body,
        LocalDateTime createdAt
) {
    public static TripMessageDto from(TripMessage m) {
        return new TripMessageDto(
                m.getId(),
                m.getTrip().getId(),
                m.getSenderUser().getId(),
                m.getSenderUser().getName(),
                m.getSenderUser().getRole().name(),
                m.getBody(),
                m.getCreatedAt()
        );
    }
}
