package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.TaxiRequest;
import com.bitfx.taxi.dto.admin.TaxiResponse;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.TaxiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/taxis")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class TaxiController {

    private final TaxiService taxiService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ApiResponse<List<TaxiResponse>> list(Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        var taxis = taxiService.listForOrg(scope).stream().map(TaxiResponse::from).toList();
        return ApiResponse.ok(taxis);
    }

    @PostMapping
    public ApiResponse<TaxiResponse> create(@Valid @RequestBody TaxiRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        return ApiResponse.ok(TaxiResponse.from(taxiService.create(req, scope)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaxiResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(TaxiResponse.from(taxiService.getById(id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaxiResponse> update(@PathVariable Long id, @Valid @RequestBody TaxiRequest req) {
        return ApiResponse.ok(TaxiResponse.from(taxiService.update(id, req)));
    }

    @PostMapping("/{id}/photo")
    public ApiResponse<TaxiResponse> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ApiResponse.ok(TaxiResponse.from(taxiService.updatePhoto(id, url)));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        taxiService.setActive(id, active);
        return ApiResponse.ok(null);
    }
}
