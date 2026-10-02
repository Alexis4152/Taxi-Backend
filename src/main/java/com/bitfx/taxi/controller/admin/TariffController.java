package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.TariffRequest;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.TariffRule;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.security.OrgScope;
import com.bitfx.taxi.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tariffs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    public ApiResponse<Long> upsert(@Valid @RequestBody TariffRequest req, Authentication authentication) {
        User actingUser = (User) authentication.getPrincipal();
        Organization scope = OrgScope.resolve(actingUser);
        TariffRule tariff = tariffService.upsert(req, scope);
        return ApiResponse.ok("Tarifa guardada", tariff.getId());
    }
}
