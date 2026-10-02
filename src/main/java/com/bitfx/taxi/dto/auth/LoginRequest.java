package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El telefono es obligatorio") String phone,
        @NotBlank(message = "La contrasena es obligatoria") String password
) {
}
