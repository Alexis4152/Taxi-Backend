package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacio")
        @Size(max = 1000, message = "El mensaje es demasiado largo")
        String body
) {
}
