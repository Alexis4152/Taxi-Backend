package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.NotNull;

public record LocationPingRequest(
        @NotNull Double lat,
        @NotNull Double lng,
        Double heading
) {
}
