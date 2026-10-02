package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.PassengerResponse;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.PassengerService;
import com.bitfx.taxi.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/passenger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PASSENGER')")
public class PassengerController {

    private final PassengerService passengerService;
    private final RatingService ratingService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public ApiResponse<PassengerResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(PassengerResponse.from(passengerService.getByUserId(user.getId())));
    }

    @GetMapping("/ratings")
    public ApiResponse<List<RatingResponse>> myRatings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(ratingService.getRatingsForUser(user.getId()));
    }

    @PostMapping("/photo")
    public ApiResponse<String> uploadPhoto(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String url = fileStorageService.store(file);
        passengerService.updatePhoto(user.getId(), url);
        return ApiResponse.ok(url);
    }
}
