package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.geocoding.GeocodeSearchResponse;
import com.bitfx.taxi.dto.geocoding.ReverseGeocodeResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Unico punto por el que la aplicacion llega a Nominatim. El frontend nunca llama al proveedor
 * de geocodificacion directamente (asi se respeta su limite de ~1 req/seg y se puede cachear o
 * cambiar de proveedor sin tocar el cliente).
 */
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping("/search")
    public ApiResponse<GeocodeSearchResponse> search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe una direccion para buscar");
        }
        var result = geocodingService.forwardGeocode(q);
        return ApiResponse.ok(GeocodeSearchResponse.from(result));
    }

    // Autocompletado: varias coincidencias (no una sola) para que el usuario elija de una lista en
    // vez de que se le llene la direccion sola con la primera que encuentre Nominatim.
    @GetMapping("/suggest")
    public ApiResponse<List<GeocodeSearchResponse>> suggest(@RequestParam String q,
                                                              @RequestParam(required = false) Double lat,
                                                              @RequestParam(required = false) Double lng) {
        if (q == null || q.trim().length() < 3) {
            return ApiResponse.ok(List.of());
        }
        var results = geocodingService.suggest(q, lat, lng).stream().map(GeocodeSearchResponse::from).toList();
        return ApiResponse.ok(results);
    }

    @GetMapping("/reverse")
    public ApiResponse<ReverseGeocodeResponse> reverse(@RequestParam double lat, @RequestParam double lng) {
        var address = geocodingService.reverseGeocode(lat, lng);
        return ApiResponse.ok(new ReverseGeocodeResponse(address));
    }
}
