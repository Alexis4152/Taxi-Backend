package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Autoregistro de operador: igual que el de pasajero, mas los datos de su taxi. Si organizationId
 * viene vacio, queda como independiente (se le asigna la organizacion especial "pool" el momento
 * de crear la cuenta). El taxi se busca por placas: si ya existe (otro operador lo trae tambien),
 * se le vincula a ese mismo; si no, se crea uno nuevo con los datos dados.
 */
public record RegisterDriverRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "^[0-9]{10}$", message = "El telefono debe tener 10 digitos")
        String phone,
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String email,
        Long organizationId,
        @NotBlank(message = "El numero de unidad es obligatorio") String taxiUnitNumber,
        @NotBlank(message = "Las placas son obligatorias") String taxiPlates,
        @NotBlank(message = "La marca del taxi es obligatoria") String taxiBrand,
        @NotBlank(message = "El modelo del taxi es obligatorio") String taxiModel,
        @AssertTrue(message = "Debes aceptar los terminos y condiciones") boolean acceptedTerms
) {
}
