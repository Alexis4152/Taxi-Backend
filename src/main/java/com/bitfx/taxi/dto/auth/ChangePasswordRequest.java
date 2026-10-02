package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres") String newPassword
) {
}
