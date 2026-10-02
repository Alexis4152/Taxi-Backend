package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPassengerRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9]{10}$", message = "El telefono debe tener 10 digitos")
        String phone,
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String email,
        @AssertTrue(message = "Debes aceptar los terminos y condiciones") boolean acceptedTerms
) {
}
