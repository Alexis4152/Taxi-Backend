package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.PassengerResponse;
import com.bitfx.taxi.dto.passenger.SavedPlaceDto;
import com.bitfx.taxi.dto.passenger.SavedPlaceResponse;
import com.bitfx.taxi.dto.passenger.TrustedContactDto;
import com.bitfx.taxi.dto.passenger.TrustedContactResponse;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.FileStorageService;
import com.bitfx.taxi.service.PassengerService;
import com.bitfx.taxi.service.RatingService;
import com.bitfx.taxi.service.SavedPlaceService;
import com.bitfx.taxi.service.TrustedContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final SavedPlaceService savedPlaceService;
    private final TrustedContactService trustedContactService;

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

    @PostMapping("/request-deletion")
    public ApiResponse<Void> requestDeletion(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        passengerService.requestOwnDeletion(user);
        return ApiResponse.ok("Tu cuenta quedo bloqueada y en revision para su eliminacion", null);
    }

    @GetMapping("/saved-places")
    public ApiResponse<List<SavedPlaceResponse>> savedPlaces(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(savedPlaceService.list(user.getId()));
    }

    @PostMapping("/saved-places")
    public ApiResponse<SavedPlaceResponse> createSavedPlace(@Valid @RequestBody SavedPlaceDto req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(savedPlaceService.create(user, req));
    }

    @DeleteMapping("/saved-places/{id}")
    public ApiResponse<Void> deleteSavedPlace(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        savedPlaceService.delete(user.getId(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/trusted-contacts")
    public ApiResponse<List<TrustedContactResponse>> trustedContacts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(trustedContactService.list(user.getId()));
    }

    @PostMapping("/trusted-contacts")
    public ApiResponse<TrustedContactResponse> createTrustedContact(@Valid @RequestBody TrustedContactDto req, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(trustedContactService.create(user, req));
    }

    @DeleteMapping("/trusted-contacts/{id}")
    public ApiResponse<Void> deleteTrustedContact(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        trustedContactService.delete(user.getId(), id);
        return ApiResponse.ok(null);
    }
}
