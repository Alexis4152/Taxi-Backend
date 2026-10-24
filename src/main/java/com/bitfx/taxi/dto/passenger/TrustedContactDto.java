package com.bitfx.taxi.dto.passenger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrustedContactDto(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80, message = "El nombre es muy largo") String name,
        @NotBlank(message = "El telefono es obligatorio") @Size(max = 20, message = "El telefono es muy largo") String phone
) {
}
