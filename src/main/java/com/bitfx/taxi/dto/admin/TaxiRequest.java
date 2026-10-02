package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record TaxiRequest(
        @NotBlank(message = "El numero de unidad es obligatorio") String unitNumber,
        @NotBlank(message = "Las placas son obligatorias") String plates,
        String brand,
        String model,
        Long organizationId
) {
}
