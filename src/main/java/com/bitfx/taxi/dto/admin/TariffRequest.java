package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TariffRequest(
        Long organizationId,
        @NotNull @Positive(message = "La tarifa base debe ser mayor a 0") BigDecimal baseFare,
        @NotNull @Positive(message = "El costo por km debe ser mayor a 0") BigDecimal perKm,
        @NotNull @Positive(message = "La tarifa minima debe ser mayor a 0") BigDecimal minFare
) {
}
