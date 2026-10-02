package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.ShiftTemplateRequest;
import com.bitfx.taxi.dto.admin.ShiftTemplateResponse;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.ShiftTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plantillas de turno (ej. "Turno Mañana" 06:00-18:00, "Turno Noche" 18:00-06:00) que el admin
 * da de alta, edita y elimina; un turno fijo debe elegir una de estas en vez de escribir horarios
 * libres cada vez.
 */
@RestController
@RequestMapping("/api/admin/shift-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    @GetMapping
    public ApiResponse<List<ShiftTemplateResponse>> list(Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var templates = shiftTemplateService.listForOrg(scope).stream().map(ShiftTemplateResponse::from).toList();
        return ApiResponse.ok(templates);
    }

    @PostMapping
    public ApiResponse<ShiftTemplateResponse> create(@Valid @RequestBody ShiftTemplateRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(ShiftTemplateResponse.from(shiftTemplateService.create(req, scope)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShiftTemplateResponse> update(@PathVariable Long id, @Valid @RequestBody ShiftTemplateRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(ShiftTemplateResponse.from(shiftTemplateService.update(id, req, scope)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        shiftTemplateService.delete(id, scope);
        return ApiResponse.ok(null);
    }
}
