package com.bitfx.taxi.service;

import com.bitfx.taxi.service.geocoding.GeocodeResult;
import com.bitfx.taxi.service.geocoding.GeocodingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fachada del "Geocoding service" que pide el frontend: el resto de la app (y los controladores)
 * solo hablan con esta clase, nunca con GeocodingProvider/Nominatim directamente. Cachea en
 * memoria (Spring Cache) para no volver a pedir la misma direccion o las mismas coordenadas -
 * Nominatim publico limita a ~1 req/seg y no debe tratarse como ilimitado.
 */
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeocodingProvider geocodingProvider;

    @Cacheable(cacheNames = "geocodeForward", key = "#query.trim().toLowerCase()")
    public GeocodeResult forwardGeocode(String query) {
        return geocodingProvider.forwardGeocode(query);
    }

    @Cacheable(cacheNames = "geocodeReverse", key = "#root.target.roundKey(#lat, #lng)")
    public String reverseGeocode(double lat, double lng) {
        return geocodingProvider.reverseGeocode(lat, lng);
    }

    // Redondea a ~11 metros de precision para que pings de GPS casi identicos compartan cache.
    public String roundKey(double lat, double lng) {
        BigDecimal rLat = BigDecimal.valueOf(lat).setScale(4, RoundingMode.HALF_UP);
        BigDecimal rLng = BigDecimal.valueOf(lng).setScale(4, RoundingMode.HALF_UP);
        return rLat + "," + rLng;
    }
}
