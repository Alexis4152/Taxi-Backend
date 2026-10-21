package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePlatformSettingsRequest(
        @NotNull(message = "El radio es obligatorio")
        @Min(value = 10, message = "El radio debe ser de al menos 10 metros")
        @Max(value = 2000, message = "El radio no puede superar 2000 metros")
        Integer tripGeofenceRadiusMeters,
        @NotNull(message = "El radio de busqueda es obligatorio")
        @DecimalMin(value = "0.5", message = "El radio debe ser de al menos 0.5 km")
        @DecimalMax(value = "50", message = "El radio no puede superar 50 km")
        Double dispatchSearchRadiusKm,
        @Size(max = 300, message = "El texto es muy largo")
        String referralRewardDescription
) {
}
