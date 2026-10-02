package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.trip.CancelTripRequest;
import com.bitfx.taxi.dto.trip.NearbyDriverDto;
import com.bitfx.taxi.dto.trip.PassengerStatsDto;
import com.bitfx.taxi.dto.trip.SendMessageRequest;
import com.bitfx.taxi.dto.trip.TripHistoryItemDto;
import com.bitfx.taxi.dto.trip.TripMessageDto;
import com.bitfx.taxi.dto.trip.TripRequestDto;
import com.bitfx.taxi.dto.trip.TripResponseDto;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.TripChatService;
import com.bitfx.taxi.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final TripChatService tripChatService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ApiResponse<TripResponseDto> request(@Valid @RequestBody TripRequestDto req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.requestTrip(user, req));
    }

    @GetMapping("/mine/active")
    @PreAuthorize("hasRole('PASSENGER')")
    public ApiResponse<TripResponseDto> myActiveTrip(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getMyActiveTripAsPassenger(user));
    }

    @GetMapping("/mine/stats")
    @PreAuthorize("hasRole('PASSENGER')")
    public ApiResponse<PassengerStatsDto> myStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getPassengerStats(user));
    }

    @GetMapping("/mine/history")
    @PreAuthorize("hasRole('PASSENGER')")
    public ApiResponse<List<TripHistoryItemDto>> myHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getMyTripHistoryAsPassenger(user));
    }

    @GetMapping("/nearby-drivers")
    @PreAuthorize("hasRole('PASSENGER')")
    public ApiResponse<List<NearbyDriverDto>> nearbyDrivers(@RequestParam double lat, @RequestParam double lng) {
        return ApiResponse.ok(tripService.listNearbyAvailableDrivers(lat, lng));
    }

    @GetMapping("/{id}")
    public ApiResponse<TripResponseDto> getStatus(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getStatus(user, id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<TripResponseDto> cancel(@PathVariable Long id, @RequestBody(required = false) CancelTripRequest req,
                                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.cancelTrip(user, id, req));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<TripMessageDto>> messages(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripChatService.listMessages(user, id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<TripMessageDto> sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest req,
                                                    Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripChatService.sendMessage(user, id, req.body()));
    }
}
