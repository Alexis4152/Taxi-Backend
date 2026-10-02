package com.bitfx.taxi.service.geocoding;

import com.bitfx.taxi.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Geocodificacion via Nominatim (OpenStreetMap). El servidor publico demo
 * (nominatim.openstreetmap.org) es gratuito y sin API key, pero su politica de uso exige:
 * maximo ~1 peticion/segundo, un User-Agent identificable, y prohibe uso pesado/comercial sin
 * infraestructura propia (https://operations.osmfoundation.org/policies/nominatim/). Por eso esta
 * clase solo se invoca desde el backend (nunca directo desde el navegador) y su resultado se
 * cachea en GeocodingService. Para produccion con trafico real, sustituir por un Nominatim propio
 * o un proveedor comercial (LocationIQ, Geoapify, OpenCage) implementando esta misma interfaz.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "nominatim", matchIfMissing = true)
public class NominatimGeocodingProvider implements GeocodingProvider {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String userAgent;

    public NominatimGeocodingProvider(@Qualifier("geocodingRestTemplate") RestTemplate restTemplate,
                                       @Value("${app.geocoding.nominatim.base-url}") String baseUrl,
                                       @Value("${app.geocoding.contact-email}") String contactEmail) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.userAgent = "BITFX-TaxiApp/1.0 (" + contactEmail + ")";
    }

    @Override
    public GeocodeResult forwardGeocode(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                .queryParam("q", query)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 0)
                .build()
                .toUriString();

        List<Map<String, Object>> results = get(url, new ParameterizedTypeReference<List<Map<String, Object>>>() {
        });

        if (results == null || results.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro esa direccion");
        }
        Map<String, Object> first = results.get(0);
        double lat = Double.parseDouble(String.valueOf(first.get("lat")));
        double lon = Double.parseDouble(String.valueOf(first.get("lon")));
        String displayName = String.valueOf(first.get("display_name"));
        return new GeocodeResult(lat, lon, displayName);
    }

    @Override
    public String reverseGeocode(double lat, double lng) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/reverse")
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .queryParam("format", "jsonv2")
                .build()
                .toUriString();

        Map<String, Object> result = get(url, new ParameterizedTypeReference<Map<String, Object>>() {
        });

        if (result == null || result.get("display_name") == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No se encontro una direccion para esas coordenadas");
        }
        return String.valueOf(result.get("display_name"));
    }

    private <T> T get(String url, ParameterizedTypeReference<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type);
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Timeout o error de red consultando Nominatim: {}", url, e);
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "El servicio de geocodificacion no respondio a tiempo");
        } catch (RestClientException e) {
            log.error("Error consultando Nominatim: {}", url, e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de geocodificacion no esta disponible en este momento");
        }
    }
}
