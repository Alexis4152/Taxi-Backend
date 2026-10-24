package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateLastLocationRequest(
        @NotNull double lat,
        @NotNull double lng
) {
}
