package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.Shift;

import java.time.LocalDateTime;

public record ShiftResponse(
        Long id,
        Long taxiId,
        String taxiUnitNumber,
        Long driverId,
        String driverName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean temporary,
        Long shiftTemplateId,
        String shiftTemplateName,
        boolean revoked,
        boolean currentlyInEffect
) {
    public static ShiftResponse from(Shift s, boolean currentlyInEffect) {
        return new ShiftResponse(
                s.getId(),
                s.getTaxi().getId(),
                s.getTaxi().getUnitNumber(),
                s.getDriver().getId(),
                s.getDriver().getUser().getName(),
                s.getStartAt(),
                s.getEndAt(),
                s.isTemporary(),
                s.getShiftTemplate() != null ? s.getShiftTemplate().getId() : null,
                s.getShiftTemplate() != null ? s.getShiftTemplate().getName() : null,
                s.isRevoked(),
                currentlyInEffect
        );
    }
}
