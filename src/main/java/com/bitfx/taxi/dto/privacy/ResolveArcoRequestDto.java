package com.bitfx.taxi.dto.privacy;

import com.bitfx.taxi.model.ArcoRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveArcoRequestDto(
        @NotNull(message = "El estatus es obligatorio") ArcoRequestStatus status,
        @Size(max = 1000, message = "La respuesta no puede pasar de 1000 caracteres") String response
) {
}
