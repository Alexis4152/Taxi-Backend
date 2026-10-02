package com.bitfx.taxi.service.route;

import com.bitfx.taxi.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Estima distancia/tiempo con la formula de Haversine (linea recta) mas un factor de vialidad,
 * sin depender de ningun servicio de red. Sirve dos propositos:
 *  1) Proveedor explicito cuando app.routing.provider=mock (util para tests o entornos sin
 *     salida a internet).
 *  2) Fallback automatico de RoutingService cuando el proveedor real (OSRM) falla o da timeout,
 *     para que un viaje nunca se quede sin estimacion de tarifa por una caida del servicio externo.
 * Por eso SIEMPRE esta registrado como bean (sin @ConditionalOnProperty): RoutingService lo
 * inyecta por tipo concreto ademas del RouteProvider "primario" que este activo.
 */
@Service
public class MockRouteProvider implements RouteProvider {

    private static final double ROAD_FACTOR = 1.35;
    private static final double AVG_SPEED_KMH = 28.0;

    @Override
    public RouteEstimate estimate(double originLat, double originLng, double destinationLat, double destinationLng) {
        double straightLineKm = GeoUtils.haversineKm(originLat, originLng, destinationLat, destinationLng);
        double roadKm = Math.max(straightLineKm * ROAD_FACTOR, 0.3);
        double minutes = (roadKm / AVG_SPEED_KMH) * 60.0;

        BigDecimal distanceKm = BigDecimal.valueOf(roadKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationMin = BigDecimal.valueOf(Math.max(minutes, 2)).setScale(2, RoundingMode.HALF_UP);
        List<List<Double>> geometry = List.of(
                List.of(originLng, originLat),
                List.of(destinationLng, destinationLat)
        );
        return new RouteEstimate(distanceKm, durationMin, geometry);
    }
}
