package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.PublicSettingsResponse;
import com.bitfx.taxi.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ajustes de la plataforma seguros de ver para cualquier usuario autenticado (no solo el admin) -
 * hoy solo si el programa de referidos esta prendido (ver ReferralCard en el front).
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping("/public")
    public ApiResponse<PublicSettingsResponse> publicSettings() {
        return ApiResponse.ok(PublicSettingsResponse.from(platformSettingsService.get()));
    }
}
