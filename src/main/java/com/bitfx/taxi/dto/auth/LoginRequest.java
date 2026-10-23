package com.bitfx.taxi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El telefono es obligatorio") String phone,
        @NotBlank(message = "La contrasena es obligatoria") String password,
        // Si ya hay una sesion abierta, el login normal la rechaza (ver AuthService.login); el
        // front se lo pregunta al usuario y, si confirma, reenvia el login con force=true para
        // cerrar esa sesion y continuar con la nueva.
        boolean force
) {
}
