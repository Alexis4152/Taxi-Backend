package com.bitfx.taxi.service.route;

import com.bitfx.taxi.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OsrmRouteProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private OsrmRouteProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OsrmRouteProvider(restTemplate, "https://router.project-osrm.org");
    }

    @Test
    void convierteLaRespuestaDeOsrmAKilometrosYMinutos() {
        Map<String, Object> geometry = Map.of("coordinates", List.of(List.of(-99.13, 19.43), List.of(-99.16, 19.45)));
        Map<String, Object> route = Map.of("distance", 5230.0, "duration", 612.0, "geometry", geometry);
        Map<String, Object> body = Map.of("code", "Ok", "routes", List.of(route));

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));

        RouteEstimate estimate = provider.estimate(19.43, -99.13, 19.45, -99.16);

        assertEquals(0, estimate.distanceKm().compareTo(java.math.BigDecimal.valueOf(5.23)));
        assertEquals(0, estimate.durationMin().compareTo(java.math.BigDecimal.valueOf(10.2)));
        assertEquals(2, estimate.routeGeometry().size());
    }

    @Test
    void lanzaNotFoundCuandoOsrmNoEncuentraRuta() {
        Map<String, Object> body = Map.of("code", "NoRoute", "routes", List.of());
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));

        ApiException ex = assertThrows(ApiException.class, () -> provider.estimate(19.43, -99.13, 19.45, -99.16));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void lanzaGatewayTimeoutCuandoOsrmNoResponde() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        ApiException ex = assertThrows(ApiException.class, () -> provider.estimate(19.43, -99.13, 19.45, -99.16));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
    }
}
