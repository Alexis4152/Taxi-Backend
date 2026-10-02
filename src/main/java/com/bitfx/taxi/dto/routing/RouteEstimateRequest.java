package com.bitfx.taxi.dto.routing;

import jakarta.validation.constraints.NotNull;

public record RouteEstimateRequest(
        @NotNull double originLat,
        @NotNull double originLng,
        @NotNull double destinationLat,
        @NotNull double destinationLng
) {
}
