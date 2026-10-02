package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaxiChangeRequestDto(
        @NotBlank(message = "El numero de unidad es obligatorio") String unitNumber,
        @NotBlank(message = "Las placas son obligatorias") String plates,
        String brand,
        String model,
        @Size(max = 300, message = "El motivo es muy largo") String reason
) {
}
