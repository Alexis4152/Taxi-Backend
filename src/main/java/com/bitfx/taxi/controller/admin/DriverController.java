package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.AdminCreateDriverRequest;
import com.bitfx.taxi.dto.admin.AdminUpdateDriverRequest;
import com.bitfx.taxi.dto.admin.DriverResponse;
import com.bitfx.taxi.dto.admin.OnlineDriverSummary;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.dto.trip.TripResponseDto;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.DriverService;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.RatingService;
import com.bitfx.taxi.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class DriverController {

    private final DriverService driverService;
    private final FileStorageService fileStorageService;
    private final RatingService ratingService;
    private final TripService tripService;

    @GetMapping
    public ApiResponse<List<DriverResponse>> list(Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var drivers = driverService.listForOrg(scope).stream().map(DriverResponse::from).toList();
        return ApiResponse.ok(drivers);
    }

    @PostMapping
    public ApiResponse<DriverResponse> create(@Valid @RequestBody AdminCreateDriverRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var driver = driverService.create(req, scope);
        return ApiResponse.ok("Operador creado, se envio la contrasena temporal por correo si se registro uno", DriverResponse.from(driver));
    }

    @GetMapping("/online")
    public ApiResponse<List<OnlineDriverSummary>> listOnline(Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(driverService.listOnline(scope));
    }

    @GetMapping("/{id}")
    public ApiResponse<DriverResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(DriverResponse.from(driverService.getById(id)));
    }

    @GetMapping("/{id}/current-trip")
    public ApiResponse<TripResponseDto> currentTrip(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(tripService.getCurrentTripForDriverAdmin(scope, id));
    }

    @GetMapping("/{id}/ratings")
    public ApiResponse<List<RatingResponse>> ratings(@PathVariable Long id, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var driver = driverService.getById(id);
        return ApiResponse.ok(ratingService.getRatingsForDriver(driver, scope));
    }

    @PutMapping("/{id}")
    public ApiResponse<DriverResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateDriverRequest req) {
        var driver = driverService.update(id, req.name(), req.email(), req.bankAccount(), req.address());
        return ApiResponse.ok("Operador actualizado", DriverResponse.from(driver));
    }

    @PostMapping("/{id}/photo")
    public ApiResponse<DriverResponse> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ApiResponse.ok(DriverResponse.from(driverService.updatePhoto(id, url)));
    }

    @PatchMapping("/{id}/bank-account")
    public ApiResponse<DriverResponse> updateBankAccount(@PathVariable Long id, @RequestParam String bankAccount) {
        return ApiResponse.ok(DriverResponse.from(driverService.updateBankAccount(id, bankAccount)));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        driverService.setActive(id, active);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/taxi")
    public ApiResponse<DriverResponse> assignTaxi(@PathVariable Long id, @RequestParam(required = false) Long taxiId,
                                                   Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(DriverResponse.from(driverService.assignTaxi(id, taxiId, scope)));
    }
}
