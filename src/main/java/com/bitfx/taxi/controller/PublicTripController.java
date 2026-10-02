package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.trip.PublicTripDto;
import com.bitfx.taxi.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint sin autenticacion para que quien recibe un enlace de "compartir viaje" pueda ver el
 * seguimiento sin necesitar cuenta; el token es opaco y de un solo viaje, nunca el id numerico.
 */
@RestController
@RequestMapping("/api/public/trips")
@RequiredArgsConstructor
public class PublicTripController {

    private final TripService tripService;

    @GetMapping("/{shareToken}")
    public ApiResponse<PublicTripDto> getPublicTrip(@PathVariable String shareToken) {
        return ApiResponse.ok(tripService.getPublicTrip(shareToken));
    }
}
