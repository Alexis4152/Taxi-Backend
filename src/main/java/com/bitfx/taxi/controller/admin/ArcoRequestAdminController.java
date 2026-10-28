package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.privacy.ArcoRequestResponse;
import com.bitfx.taxi.dto.privacy.ResolveArcoRequestDto;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.ArcoRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bandeja de solicitudes ARCO. Solo el super admin: los datos personales son responsabilidad de la
 * plataforma (NexoraTaxis), no de cada organizacion de taxis.
 */
@RestController
@RequestMapping("/api/admin/arco-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ArcoRequestAdminController {

    private final ArcoRequestService arcoRequestService;

    @GetMapping
    public ApiResponse<List<ArcoRequestResponse>> list() {
        return ApiResponse.ok(arcoRequestService.listAll());
    }

    @PatchMapping("/{id}")
    public ApiResponse<ArcoRequestResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolveArcoRequestDto req,
                                                    Authentication authentication) {
        return ApiResponse.ok("Solicitud actualizada",
                arcoRequestService.resolve(id, req.status(), req.response(), (User) authentication.getPrincipal()));
    }
}
