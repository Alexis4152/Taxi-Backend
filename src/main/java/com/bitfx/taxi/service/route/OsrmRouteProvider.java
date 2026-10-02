package com.bitfx.taxi.service.route;

import com.bitfx.taxi.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ruteo real via OSRM (Open Source Routing Machine), usando por defecto el servidor demo publico
 * router.project-osrm.org: gratis, sin API key, pero el propio proyecto lo documenta como "demo,
 * no apto para produccion" (sin SLA ni garantia de disponibilidad). Por eso RoutingService
 * envuelve este proveedor con cache + fallback a MockRouteProvider, y por eso este bean es
 * intercambiable: para produccion, apuntar OSRM_BASE_URL a un OSRM propio (Docker oficial) o
 * reemplazar por otra implementacion de RouteProvider (GraphHopper/Valhalla) sin tocar
 * TripService/FareService/el frontend.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.routing.provider", havingValue = "osrm", matchIfMissing = true)
public class OsrmRouteProvider implements RouteProvider {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OsrmRouteProvider(@Qualifier("routingRestTemplate") RestTemplate restTemplate,
                              @Value("${app.routing.osrm.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RouteEstimate estimate(double originLat, double originLng, double destinationLat, double destinationLng) {
        String coordinates = String.format(Locale.US, "%f,%f;%f,%f", originLng, originLat, destinationLng, destinationLat);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/route/v1/driving/" + coordinates)
                .queryParam("overview", "full")
                .queryParam("geometries", "geojson")
                .build()
                .toUriString();

        Map<String, Object> body;
        try {
            body = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }).getBody();
        } catch (ResourceAccessException e) {
            log.error("Timeout o error de red consultando OSRM: {}", url, e);
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "El servicio de rutas no respondio a tiempo");
        } catch (RestClientException e) {
            log.error("Error consultando OSRM: {}", url, e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de rutas no esta disponible en este momento");
        }

        if (body == null || !"Ok".equals(body.get("code"))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro una ruta entre esos puntos");
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro una ruta entre esos puntos");
        }

        Map<String, Object> route = routes.get(0);
        double meters = ((Number) route.get("distance")).doubleValue();
        double seconds = ((Number) route.get("duration")).doubleValue();

        Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");
        List<List<Double>> coords = ((List<List<Number>>) geometry.get("coordinates")).stream()
                .map(pair -> List.of(pair.get(0).doubleValue(), pair.get(1).doubleValue()))
                .toList();

        BigDecimal distanceKm = BigDecimal.valueOf(meters / 1000.0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationMin = BigDecimal.valueOf(seconds / 60.0).setScale(2, RoundingMode.HALF_UP);
        return new RouteEstimate(distanceKm, durationMin, coords);
    }
}
