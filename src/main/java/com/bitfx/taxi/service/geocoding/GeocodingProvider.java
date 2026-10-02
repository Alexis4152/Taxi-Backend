package com.bitfx.taxi.service.geocoding;

/**
 * Abstraccion del proveedor de geocodificacion (directa e inversa). El resto de la aplicacion
 * (frontend incluido) solo conoce esta interfaz, nunca el proveedor concreto (Nominatim hoy;
 * manana podria ser un Nominatim propio, LocationIQ, Geoapify, OpenCage, etc.).
 */
public interface GeocodingProvider {

    GeocodeResult forwardGeocode(String query);

    String reverseGeocode(double lat, double lng);
}
