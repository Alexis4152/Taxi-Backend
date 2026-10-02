package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.Rating;

import java.time.LocalDateTime;

public record RatingResponse(
        Long id,
        Long tripId,
        String direction,
        int score,
        String comment,
        LocalDateTime createdAt,
        String fromUserName
) {
    public static RatingResponse from(Rating r) {
        return new RatingResponse(
                r.getId(),
                r.getTrip().getId(),
                r.getDirection().name(),
                r.getScore(),
                r.getComment(),
                r.getCreatedAt(),
                r.getFromUser().getName()
        );
    }
}
