package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.service.route.MockRouteProvider;
import com.bitfx.taxi.service.route.RouteEstimate;
import com.bitfx.taxi.service.route.RouteProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fachada del "Routing service" que pide el frontend: TripService/controladores solo hablan con
 * esta clase, nunca con RouteProvider/OSRM directamente. Dos responsabilidades ademas de delegar:
 *  - Cache: pares origen/destino repetidos (ej. reintentos del usuario) no vuelven a pedir red.
 *  - Resiliencia: si el proveedor primario (OSRM) falla o da timeout, cae automaticamente al
 *    calculo local por Haversine para que un problema del servicio externo nunca deje al pasajero
 *    sin tarifa estimada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RouteProvider primaryProvider;
    private final MockRouteProvider fallbackProvider;

    @Cacheable(cacheNames = "routeEstimate", key = "#root.target.roundKey(#originLat, #originLng, #destinationLat, #destinationLng)")
    public RouteEstimate estimate(double originLat, double originLng, double destinationLat, double destinationLng) {
        try {
            return primaryProvider.estimate(originLat, originLng, destinationLat, destinationLng);
        } catch (ApiException e) {
            // "No se encontro ruta" es una respuesta valida del proveedor, no una falla de
            // infraestructura: se propaga tal cual en vez de disfrazarla con una linea recta.
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw e;
            }
            log.warn("El proveedor de rutas primario no esta disponible, usando calculo de respaldo (Haversine): {}", e.getMessage());
            return fallbackProvider.estimate(originLat, originLng, destinationLat, destinationLng);
        } catch (Exception e) {
            log.warn("Fallo inesperado del proveedor de rutas primario, usando calculo de respaldo (Haversine)", e);
            return fallbackProvider.estimate(originLat, originLng, destinationLat, destinationLng);
        }
    }

    // Redondea a ~11 metros para que pings de GPS casi identicos reutilicen la misma entrada de cache.
    public String roundKey(double originLat, double originLng, double destinationLat, double destinationLng) {
        return round(originLat) + "," + round(originLng) + ";" + round(destinationLat) + "," + round(destinationLng);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
