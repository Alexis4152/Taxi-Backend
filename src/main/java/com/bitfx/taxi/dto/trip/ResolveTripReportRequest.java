package com.bitfx.taxi.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveTripReportRequest(
        @NotBlank(message = "Describe como se resolvio el reporte") @Size(max = 500, message = "El comentario es muy largo") String comment
) {
}
