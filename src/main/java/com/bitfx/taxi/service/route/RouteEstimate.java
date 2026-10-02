package com.bitfx.taxi.service.route;

import java.math.BigDecimal;
import java.util.List;

/**
 * routeGeometry: lista de puntos [lng, lat] (orden GeoJSON/MapLibre) que dibujan la ruta sobre
 * el mapa. No se persiste en base de datos (es un dato derivado, barato de recalcular); solo
 * viaja en la respuesta de la API para que el frontend la dibuje.
 */
public record RouteEstimate(BigDecimal distanceKm, BigDecimal durationMin, List<List<Double>> routeGeometry) {
}
