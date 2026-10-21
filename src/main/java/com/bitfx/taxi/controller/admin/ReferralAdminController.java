package com.bitfx.taxi.controller.admin;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.referral.ReferralEntryResponse;
import com.bitfx.taxi.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/referrals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ReferralAdminController {

    private final ReferralService referralService;

    @GetMapping
    public ApiResponse<List<ReferralEntryResponse>> list() {
        return ApiResponse.ok(referralService.listAll());
    }
}
