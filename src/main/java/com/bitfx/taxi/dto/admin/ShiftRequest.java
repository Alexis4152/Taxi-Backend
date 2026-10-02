package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull(message = "El taxi es obligatorio") Long taxiId,
        @NotNull(message = "El operador es obligatorio") Long driverId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean temporary,
        // Obligatorio para turnos fijos (define su horario diario); no aplica a temporales.
        Long shiftTemplateId
) {
}
