package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTripDestinationRequest(
        @NotNull double destinationLat,
        @NotNull double destinationLng,
        @NotBlank(message = "La direccion de destino es obligatoria") String destinationAddress
) {
}
