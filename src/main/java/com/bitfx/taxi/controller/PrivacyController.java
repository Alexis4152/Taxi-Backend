package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.privacy.ArcoRequestResponse;
import com.bitfx.taxi.dto.privacy.CreateArcoRequestDto;
import com.bitfx.taxi.dto.privacy.PrivacyPreferencesRequest;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.ArcoRequestService;
import com.bitfx.taxi.service.PrivacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Autoservicio de privacidad para pasajeros y operadores (titulares de los datos): aviso de
 * privacidad, preferencias, descarga de sus datos y solicitudes ARCO.
 */
@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PASSENGER','DRIVER')")
public class PrivacyController {

    private final PrivacyService privacyService;
    private final ArcoRequestService arcoRequestService;

    @PostMapping("/accept")
    public ApiResponse<Void> acceptPrivacyNotice(@RequestBody PrivacyPreferencesRequest req, Authentication authentication) {
        privacyService.acceptPrivacyNotice((User) authentication.getPrincipal(), req.marketingOptIn());
        return ApiResponse.ok(null);
    }

    @PutMapping("/preferences")
    public ApiResponse<Void> updatePreferences(@RequestBody PrivacyPreferencesRequest req, Authentication authentication) {
        privacyService.updatePreferences((User) authentication.getPrincipal(), req.marketingOptIn());
        return ApiResponse.ok("Preferencias actualizadas", null);
    }

    @GetMapping("/my-data")
    public ApiResponse<Map<String, Object>> myData(Authentication authentication) {
        return ApiResponse.ok(privacyService.exportMyData((User) authentication.getPrincipal()));
    }

    @GetMapping("/arco-requests")
    public ApiResponse<List<ArcoRequestResponse>> myArcoRequests(Authentication authentication) {
        return ApiResponse.ok(arcoRequestService.listMine((User) authentication.getPrincipal()));
    }

    @PostMapping("/arco-requests")
    public ApiResponse<ArcoRequestResponse> createArcoRequest(@Valid @RequestBody CreateArcoRequestDto req, Authentication authentication) {
        var request = arcoRequestService.create((User) authentication.getPrincipal(), req.type(), req.description());
        return ApiResponse.ok("Solicitud registrada con folio " + request.folio(), request);
    }
}
