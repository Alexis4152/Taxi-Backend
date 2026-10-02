package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.Organization;

public record OrganizationResponse(Long id, String name, String contactPhone, String logoUrl, boolean active) {
    public static OrganizationResponse from(Organization o) {
        return new OrganizationResponse(o.getId(), o.getName(), o.getContactPhone(), o.getLogoUrl(), o.isActive());
    }
}
