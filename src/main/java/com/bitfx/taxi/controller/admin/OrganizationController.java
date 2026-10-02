package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.OrganizationRequest;
import com.bitfx.taxi.dto.admin.OrganizationResponse;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(OrganizationResponse.from(organizationService.getById(id)));
    }

    @PostMapping("/{id}/logo")
    public ApiResponse<OrganizationResponse> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ApiResponse.ok(OrganizationResponse.from(organizationService.updateLogo(id, url)));
    }

    @GetMapping
    public ApiResponse<List<OrganizationResponse>> list() {
        var orgs = organizationService.listAll().stream().map(OrganizationResponse::from).toList();
        return ApiResponse.ok(orgs);
    }

    @PostMapping
    public ApiResponse<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest req) {
        return ApiResponse.ok(OrganizationResponse.from(organizationService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<OrganizationResponse> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest req) {
        return ApiResponse.ok(OrganizationResponse.from(organizationService.update(id, req)));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<Void> setActive(@PathVariable Long id, @RequestParam boolean active) {
        organizationService.setActive(id, active);
        return ApiResponse.ok(null);
    }
}
