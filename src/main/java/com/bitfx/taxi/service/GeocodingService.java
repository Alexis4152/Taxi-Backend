package com.bitfx.taxi.service;

import com.bitfx.taxi.service.geocoding.GeocodeResult;
import com.bitfx.taxi.service.geocoding.GeocodingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    private static final int SUGGEST_LIMIT = 6;

    @Cacheable(cacheNames = "geocodeSuggest", key = "#root.target.suggestKey(#query, #nearLat, #nearLng)")
    public List<GeocodeResult> suggest(String query, Double nearLat, Double nearLng) {
        return geocodingProvider.suggest(query, nearLat, nearLng, SUGGEST_LIMIT);
    }

    // La clave incluye la coordenada (redondeada, ver roundKey) ademas del texto: dos personas en
    // ciudades distintas que escriben la misma palabra no deben compartir cache entre si.
    public String suggestKey(String query, Double nearLat, Double nearLng) {
        String coordPart = (nearLat != null && nearLng != null) ? roundKey(nearLat, nearLng) : "none";
        return query.trim().toLowerCase() + "|" + coordPart;
    }

    // Redondea a ~11 metros de precision para que pings de GPS casi identicos compartan cache.
    public String roundKey(double lat, double lng) {
        BigDecimal rLat = BigDecimal.valueOf(lat).setScale(4, RoundingMode.HALF_UP);
        BigDecimal rLng = BigDecimal.valueOf(lng).setScale(4, RoundingMode.HALF_UP);
        return rLat + "," + rLng;
    }
}
