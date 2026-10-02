package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.trip.RatingRequest;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PASSENGER','DRIVER')")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ApiResponse<RatingResponse> submit(@Valid @RequestBody RatingRequest req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(RatingResponse.from(ratingService.submit(user, req)));
    }
}
