package com.bitfx.taxi.dto;

import com.bitfx.taxi.model.PlatformSettings;

/**
 * Subconjunto de PlatformSettings seguro de exponer a cualquier usuario autenticado (operador o
 * pasajero), a diferencia de PlatformSettingsResponse que es solo para el admin.
 */
public record PublicSettingsResponse(boolean referralProgramEnabled) {
    public static PublicSettingsResponse from(PlatformSettings s) {
        return new PublicSettingsResponse(s.isReferralProgramEnabled());
    }
}
