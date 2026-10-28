package com.bitfx.taxi.dto.privacy;

import com.bitfx.taxi.model.ArcoRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateArcoRequestDto(
        @NotNull(message = "Elige el derecho que quieres ejercer") ArcoRequestType type,
        @NotBlank(message = "Describe tu solicitud")
        @Size(max = 1000, message = "La descripcion no puede pasar de 1000 caracteres")
        String description
) {
}
