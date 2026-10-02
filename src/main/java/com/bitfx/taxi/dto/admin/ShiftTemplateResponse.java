package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.ShiftTemplate;

import java.time.LocalTime;

public record ShiftTemplateResponse(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean active,
        Long organizationId,
        String organizationName
) {
    public static ShiftTemplateResponse from(ShiftTemplate t) {
        return new ShiftTemplateResponse(
                t.getId(), t.getName(), t.getStartTime(), t.getEndTime(), t.isActive(),
                t.getOrganization().getId(), t.getOrganization().getName()
        );
    }
}
