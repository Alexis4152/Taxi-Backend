package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTripOriginRequest(
        @NotNull double originLat,
        @NotNull double originLng,
        @NotBlank(message = "La direccion de origen es obligatoria") String originAddress
) {
}
