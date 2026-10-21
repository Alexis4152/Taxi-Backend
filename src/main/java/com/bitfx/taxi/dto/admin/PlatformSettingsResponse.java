package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.PlatformSettings;

public record PlatformSettingsResponse(int tripGeofenceRadiusMeters) {
    public static PlatformSettingsResponse from(PlatformSettings s) {
        return new PlatformSettingsResponse(s.getTripGeofenceRadiusMeters());
    }
}
