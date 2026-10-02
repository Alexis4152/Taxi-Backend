package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminUpdateDriverRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @Email(message = "Correo invalido") String email,
        String bankAccount,
        String address
) {
}
