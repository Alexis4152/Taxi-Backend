package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePlatformSettingsRequest(
        @NotNull(message = "El radio es obligatorio")
        @Min(value = 10, message = "El radio debe ser de al menos 10 metros")
        @Max(value = 2000, message = "El radio no puede superar 2000 metros")
        Integer tripGeofenceRadiusMeters
) {
}
