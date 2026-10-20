package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.TripReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TripReportDto(
        @NotNull(message = "La categoria es obligatoria") TripReportCategory category,
        @NotBlank(message = "Describe brevemente el problema") @Size(max = 500, message = "La descripcion es muy larga") String description
) {
}
