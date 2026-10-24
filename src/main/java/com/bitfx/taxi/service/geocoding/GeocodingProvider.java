package com.bitfx.taxi.service.geocoding;

import java.util.List;

/**
 * Abstraccion del proveedor de geocodificacion (directa e inversa). El resto de la aplicacion
 * (frontend incluido) solo conoce esta interfaz, nunca el proveedor concreto (Nominatim hoy;
 * manana podria ser un Nominatim propio, LocationIQ, Geoapify, OpenCage, etc.).
 */
public interface GeocodingProvider {

    GeocodeResult forwardGeocode(String query);

    String reverseGeocode(double lat, double lng);

    // Variante para autocompletado: varias coincidencias, con preferencia (no restriccion) por las
    // cercanas a nearLat/nearLng si se dan.
    List<GeocodeResult> suggest(String query, Double nearLat, Double nearLng, int limit);
}
