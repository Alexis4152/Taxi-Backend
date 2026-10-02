package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.ShiftRequest;
import com.bitfx.taxi.dto.admin.ShiftResponse;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.DriverService;
import com.bitfx.taxi.service.ShiftService;
import com.bitfx.taxi.service.TaxiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ShiftController {

    private final ShiftService shiftService;
    private final TaxiService taxiService;
    private final DriverService driverService;

    @GetMapping("/by-taxi/{taxiId}")
    public ApiResponse<List<ShiftResponse>> listForTaxi(@PathVariable Long taxiId) {
        var taxi = taxiService.getById(taxiId);
        var shifts = shiftService.listForTaxi(taxi).stream()
                .map(s -> ShiftResponse.from(s, shiftService.isCurrentlyInEffect(s)))
                .toList();
        return ApiResponse.ok(shifts);
    }

    @GetMapping("/by-driver/{driverId}")
    public ApiResponse<List<ShiftResponse>> listForDriver(@PathVariable Long driverId) {
        var driver = driverService.getById(driverId);
        var shifts = shiftService.listForDriver(driver).stream()
                .map(s -> ShiftResponse.from(s, shiftService.isCurrentlyInEffect(s)))
                .toList();
        return ApiResponse.ok(shifts);
    }

    @PostMapping
    public ApiResponse<ShiftResponse> assign(@Valid @RequestBody ShiftRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var shift = shiftService.assign(req, actingUser, scope);
        return ApiResponse.ok("Turno asignado correctamente", ShiftResponse.from(shift, shiftService.isCurrentlyInEffect(shift)));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<Void> end(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        shiftService.end(id, scope);
        return ApiResponse.ok(null);
    }
}
