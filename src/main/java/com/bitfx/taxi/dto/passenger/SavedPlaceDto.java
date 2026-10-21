package com.bitfx.taxi.dto.passenger;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavedPlaceDto(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 40, message = "El nombre es muy largo") String label,
        @NotBlank(message = "La direccion es obligatoria") @Size(max = 255, message = "La direccion es muy larga") String address,
        @NotNull @DecimalMin(value = "-90", message = "Latitud invalida") @DecimalMax(value = "90", message = "Latitud invalida") Double lat,
        @NotNull @DecimalMin(value = "-180", message = "Longitud invalida") @DecimalMax(value = "180", message = "Longitud invalida") Double lng
) {
}
