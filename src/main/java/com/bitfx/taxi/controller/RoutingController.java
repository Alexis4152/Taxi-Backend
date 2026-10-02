package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.routing.RouteEstimateRequest;
import com.bitfx.taxi.dto.routing.RouteEstimateResponse;
import com.bitfx.taxi.service.FareService;
import com.bitfx.taxi.service.RoutingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preview de ruta/tarifa ANTES de solicitar el viaje (pasos 5-8 del flujo del pasajero: distancia,
 * duracion, ruta y costo se muestran antes de tocar "solicitar taxi"). No crea ningun Trip; el
 * calculo real y definitivo se repite en TripService.requestTrip al confirmar.
 */
@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingService routingService;
    private final FareService fareService;

    @PostMapping("/estimate")
    public ApiResponse<RouteEstimateResponse> estimate(@Valid @RequestBody RouteEstimateRequest req) {
        var estimate = routingService.estimate(req.originLat(), req.originLng(), req.destinationLat(), req.destinationLng());
        var fare = fareService.estimateFare(null, estimate.distanceKm());
        return ApiResponse.ok(new RouteEstimateResponse(estimate.distanceKm(), estimate.durationMin(), fare, estimate.routeGeometry()));
    }
}
