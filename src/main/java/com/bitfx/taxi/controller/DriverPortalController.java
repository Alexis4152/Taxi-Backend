package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.admin.DriverResponse;
import com.bitfx.taxi.dto.auth.CurrentTaxiSummary;
import com.bitfx.taxi.dto.trip.DriverStatsDto;
import com.bitfx.taxi.dto.trip.LocationPingRequest;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.dto.trip.TripHistoryItemDto;
import com.bitfx.taxi.dto.trip.TripResponseDto;
import com.bitfx.taxi.dto.admin.TaxiChangeRequestDto;
import com.bitfx.taxi.dto.admin.TaxiChangeRequestResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.LocationService;
import com.bitfx.taxi.service.RatingService;
import com.bitfx.taxi.service.TaxiChangeRequestService;
import com.bitfx.taxi.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
public class DriverPortalController {

    private final LocationService locationService;
    private final TripService tripService;
    private final RatingService ratingService;
    private final DriverProfileRepository driverProfileRepository;
    private final TaxiChangeRequestService taxiChangeRequestService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public ApiResponse<DriverResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        return ApiResponse.ok(DriverResponse.from(driver));
    }

    @PostMapping("/photo")
    public ApiResponse<String> uploadPhoto(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        String url = fileStorageService.store(file);
        driver.setPhotoUrl(url);
        driverProfileRepository.save(driver);
        return ApiResponse.ok(url);
    }

    @GetMapping("/my-taxi")
    public ApiResponse<CurrentTaxiSummary> myTaxi(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        var currentTaxi = driver.getTaxi() != null
                ? new CurrentTaxiSummary(driver.getTaxi().getId(), driver.getTaxi().getUnitNumber(), driver.getTaxi().getPlates())
                : null;
        return ApiResponse.ok(currentTaxi);
    }

    @PostMapping("/taxi-change-requests")
    public ApiResponse<TaxiChangeRequestResponse> requestTaxiChange(@Valid @RequestBody TaxiChangeRequestDto req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok("Tu solicitud fue enviada, te avisaremos por correo en cuanto se resuelva", taxiChangeRequestService.submit(user, req));
    }

    @GetMapping("/taxi-change-requests")
    public ApiResponse<List<TaxiChangeRequestResponse>> myTaxiChangeRequests(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(taxiChangeRequestService.listForDriver(user));
    }

    @GetMapping("/stats")
    public ApiResponse<DriverStatsDto> stats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getDriverStats(user));
    }

    @GetMapping("/ratings")
    public ApiResponse<List<RatingResponse>> myRatings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(ratingService.getRatingsForUser(user.getId()));
    }

    @GetMapping("/trips/history")
    public ApiResponse<List<TripHistoryItemDto>> tripHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getMyTripHistoryAsDriver(user));
    }

    @PostMapping("/location")
    public ApiResponse<Void> ping(@Valid @RequestBody LocationPingRequest req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        locationService.ping(user, req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/offline")
    public ApiResponse<Void> goOffline(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        locationService.goOffline(user);
        return ApiResponse.ok(null);
    }

    @GetMapping("/trips/active")
    public ApiResponse<TripResponseDto> activeTrip(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.getMyActiveTripAsDriver(user));
    }

    @PostMapping("/trips/{tripId}/offers/{offerId}/accept")
    public ApiResponse<TripResponseDto> accept(@PathVariable Long tripId, @PathVariable Long offerId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.acceptOffer(user, tripId, offerId));
    }

    @PostMapping("/trips/{tripId}/offers/{offerId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long tripId, @PathVariable Long offerId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        tripService.rejectOffer(user, tripId, offerId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/trips/{tripId}/start")
    public ApiResponse<TripResponseDto> start(@PathVariable Long tripId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.startTrip(user, tripId));
    }

    @PostMapping("/trips/{tripId}/complete")
    public ApiResponse<TripResponseDto> complete(@PathVariable Long tripId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(tripService.completeTrip(user, tripId));
    }

    @PostMapping("/trips/{tripId}/confirm-payment")
    public ApiResponse<Void> confirmPayment(@PathVariable Long tripId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        tripService.confirmPayment(user, tripId);
        return ApiResponse.ok(null);
    }
}
