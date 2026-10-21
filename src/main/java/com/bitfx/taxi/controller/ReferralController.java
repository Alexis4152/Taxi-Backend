package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.referral.MyReferralResponse;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/me")
    public ApiResponse<MyReferralResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(referralService.getMyReferralInfo(user));
    }
}
