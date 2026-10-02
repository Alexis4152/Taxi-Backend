package com.bitfx.taxi.dto.auth;

import com.bitfx.taxi.model.Role;
import com.bitfx.taxi.model.User;

public record UserSummary(
        Long id,
        String name,
        String phone,
        String email,
        Role role,
        Long organizationId,
        String organizationName,
        String organizationLogoUrl,
        boolean mustChangePassword,
        Long driverId,
        CurrentTaxiSummary currentTaxi
) {
    public static UserSummary from(User user) {
        return from(user, null, null);
    }

    public static UserSummary from(User user, Long driverId, CurrentTaxiSummary currentTaxi) {
        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getOrganization() != null ? user.getOrganization().getName() : null,
                user.getOrganization() != null ? user.getOrganization().getLogoUrl() : null,
                user.isMustChangePassword(),
                driverId,
                currentTaxi
        );
    }
}
