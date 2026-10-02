package com.bitfx.taxi.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        String contactPhone
) {
}
