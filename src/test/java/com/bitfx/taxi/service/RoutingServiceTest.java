package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.service.route.MockRouteProvider;
import com.bitfx.taxi.service.route.RouteEstimate;
import com.bitfx.taxi.service.route.RouteProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private RouteProvider primaryProvider;

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService(primaryProvider, new MockRouteProvider());
    }

    @Test
    void usaElProveedorPrimarioCuandoRespondeBien() {
        RouteEstimate expected = new RouteEstimate(BigDecimal.valueOf(5.0), BigDecimal.valueOf(12.0), List.of());
        when(primaryProvider.estimate(19.0, -99.0, 19.1, -99.1)).thenReturn(expected);

        RouteEstimate result = routingService.estimate(19.0, -99.0, 19.1, -99.1);

        assertEquals(expected, result);
    }

    @Test
    void caeAlProveedorDeRespaldoSiElPrimarioNoEstaDisponible() {
        when(primaryProvider.estimate(19.0, -99.0, 19.1, -99.1))
                .thenThrow(new ResourceAccessException("timeout"));

        RouteEstimate result = routingService.estimate(19.0, -99.0, 19.1, -99.1);

        assertNotNull(result);
        assertTrue(result.distanceKm().doubleValue() > 0);
    }

    @Test
    void noOcultaUnaRutaGenuinamenteNoEncontrada() {
        when(primaryProvider.estimate(19.0, -99.0, 19.1, -99.1))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "No se encontro una ruta entre esos puntos"));

        ApiException ex = assertThrows(ApiException.class, () -> routingService.estimate(19.0, -99.0, 19.1, -99.1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
