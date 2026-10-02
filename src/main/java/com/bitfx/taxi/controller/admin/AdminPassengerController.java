package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.AdminPassengerResponse;
import com.bitfx.taxi.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Los pasajeros no pertenecen a ninguna organizacion (son clientes de la plataforma completa), asi
 * que solo el super admin los administra - no hay un "scope" de organizacion que aplicar aqui.
 */
@RestController
@RequestMapping("/api/admin/passengers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminPassengerController {

    private final PassengerService passengerService;

    @GetMapping
    public ApiResponse<List<AdminPassengerResponse>> list() {
        return ApiResponse.ok(passengerService.listAll().stream().map(AdminPassengerResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminPassengerResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(AdminPassengerResponse.from(passengerService.getById(id)));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        passengerService.setActive(id, active);
        return ApiResponse.ok(null);
    }
}
