package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.PlatformSettingsResponse;
import com.bitfx.taxi.dto.admin.UpdatePlatformSettingsRequest;
import com.bitfx.taxi.service.PlatformSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Solo SUPER_ADMIN (no ADMIN de organizacion): estos ajustes aplican a toda la plataforma, no a
 * una sola organizacion.
 */
@RestController
@RequestMapping("/api/admin/platform-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    public ApiResponse<PlatformSettingsResponse> get() {
        return ApiResponse.ok(PlatformSettingsResponse.from(platformSettingsService.get()));
    }

    @PutMapping
    public ApiResponse<PlatformSettingsResponse> update(@Valid @RequestBody UpdatePlatformSettingsRequest req) {
        var updated = platformSettingsService.update(
                req.tripGeofenceRadiusMeters(), req.dispatchSearchRadiusKm(), req.referralRewardDescription());
        return ApiResponse.ok("Configuracion actualizada", PlatformSettingsResponse.from(updated));
    }
}
