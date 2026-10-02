package com.bitfx.taxi.service.geocoding;

import com.bitfx.taxi.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NominatimGeocodingProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private NominatimGeocodingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new NominatimGeocodingProvider(restTemplate, "https://nominatim.openstreetmap.org", "test@bitfx.mx");
    }

    @Test
    void convierteUnaDireccionEnCoordenadas() {
        List<Map<String, Object>> body = List.of(Map.of("lat", "19.4326", "lon", "-99.1332", "display_name", "Zocalo, CDMX"));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));

        GeocodeResult result = provider.forwardGeocode("Zocalo, CDMX");

        assertEquals(19.4326, result.lat(), 0.0001);
        assertEquals(-99.1332, result.lng(), 0.0001);
        assertEquals("Zocalo, CDMX", result.displayName());
    }

    @Test
    void lanzaNotFoundSiNoHayResultados() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        ApiException ex = assertThrows(ApiException.class, () -> provider.forwardGeocode("direccion inexistente xyz"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void convierteCoordenadasEnDireccion() {
        Map<String, Object> body = Map.of("display_name", "Av. Reforma, CDMX");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));

        String address = provider.reverseGeocode(19.43, -99.16);

        assertEquals("Av. Reforma, CDMX", address);
    }

    @Test
    void lanzaGatewayTimeoutCuandoNominatimNoResponde() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        ApiException ex = assertThrows(ApiException.class, () -> provider.reverseGeocode(19.43, -99.16));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
    }
}
