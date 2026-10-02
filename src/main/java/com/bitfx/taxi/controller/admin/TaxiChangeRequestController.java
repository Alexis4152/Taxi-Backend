package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.TaxiChangeRequestResponse;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.TaxiChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/taxi-change-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class TaxiChangeRequestController {

    private final TaxiChangeRequestService taxiChangeRequestService;

    @GetMapping
    public ApiResponse<List<TaxiChangeRequestResponse>> listPending(Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(taxiChangeRequestService.listPending(scope));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<TaxiChangeRequestResponse> approve(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(taxiChangeRequestService.resolve(id, true, actingUser, scope));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TaxiChangeRequestResponse> reject(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(taxiChangeRequestService.resolve(id, false, actingUser, scope));
    }
}
