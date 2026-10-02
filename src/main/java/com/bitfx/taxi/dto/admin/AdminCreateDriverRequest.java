package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminCreateDriverRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9]{10}$", message = "El telefono debe tener 10 digitos")
        String phone,
        @Email(message = "Correo invalido") String email,
        String bankAccount,
        String address,
        Long organizationId,
        // Opcional: si se indica, el operador queda vinculado de una vez a ese taxi.
        Long taxiId
) {
}
