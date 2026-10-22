package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.trip.ResolveTripReportRequest;
import com.bitfx.taxi.dto.trip.TripReportResponse;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.TripReportStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.TripReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trip-reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class TripReportController {

    private final TripReportService tripReportService;

    @GetMapping
    public ApiResponse<List<TripReportResponse>> list(@RequestParam(required = false) TripReportStatus status, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(tripReportService.list(scope, status));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<TripReportResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolveTripReportRequest req,
                                                    Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok("Reporte resuelto, se notifico a ambas partes", tripReportService.resolve(id, actingUser, scope, req));
    }
}
