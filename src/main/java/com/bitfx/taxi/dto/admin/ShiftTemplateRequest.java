package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ShiftTemplateRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime startTime,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime endTime,
        Long organizationId
) {
}
