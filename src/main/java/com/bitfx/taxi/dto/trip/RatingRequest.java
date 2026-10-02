package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RatingRequest(
        @NotNull(message = "El viaje es obligatorio") Long tripId,
        @NotNull @Min(1) @Max(5) Integer score,
        String comment,
        @DecimalMin(value = "0", message = "La propina no puede ser negativa") BigDecimal tip
) {
}
