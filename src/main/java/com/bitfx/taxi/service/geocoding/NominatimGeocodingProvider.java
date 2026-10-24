package com.bitfx.taxi.service.geocoding;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.util.GeoUtils;
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
                .encode()
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
    public List<GeocodeResult> suggest(String query, Double nearLat, Double nearLng, int limit) {
        int finalLimit = Math.max(1, Math.min(limit, 8));
        // Se piden mas candidatos de los que se van a mostrar: Nominatim los ordena por relevancia
        // textual/importancia, no por cercania, asi que si solo pidieramos `finalLimit` nos
        // arriesgamos a que el resultado realmente cercano ni siquiera llegue a estar entre ellos.
        // Pidiendo mas y reordenando nosotros por distancia (ver abajo) lo "preferimos" de verdad
        // sin excluir los lejanos (siguen apareciendo, solo mas abajo en la lista).
        // Nominatim ordena sus resultados por relevancia/importancia global primero: para un
        // termino muy comun (ej. "periferico"), el tramo "importante" de otra ciudad puede ganarle
        // facil al tramo local aunque este mas cerca, y si no pedimos suficientes candidatos ese
        // tramo local nunca llega siquiera a estar entre los que reordenamos por cercania abajo.
        // 40 es el maximo que permite la API publica de Nominatim.
        int fetchLimit = nearLat != null && nearLng != null ? 40 : finalLimit;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                .queryParam("q", query)
                .queryParam("format", "jsonv2")
                .queryParam("limit", fetchLimit)
                .queryParam("addressdetails", 0)
                .queryParam("countrycodes", "mx");
        if (nearLat != null && nearLng != null) {
            // Caja de ~20km alrededor del punto, solo para PREFERIR resultados cercanos
            // (bounded=0 = no los excluye, nada mas los prioriza) - no restringe la busqueda a esa zona.
            double delta = 0.2;
            String viewbox = (nearLng - delta) + "," + (nearLat + delta) + "," + (nearLng + delta) + "," + (nearLat - delta);
            builder.queryParam("viewbox", viewbox).queryParam("bounded", 0);
        }
        String url = builder.build().encode().toUriString();

        List<Map<String, Object>> results = get(url, new ParameterizedTypeReference<List<Map<String, Object>>>() {
        });
        if (results == null) {
            return List.of();
        }
        List<GeocodeResult> mapped = results.stream()
                .map(r -> new GeocodeResult(
                        Double.parseDouble(String.valueOf(r.get("lat"))),
                        Double.parseDouble(String.valueOf(r.get("lon"))),
                        String.valueOf(r.get("display_name"))))
                .toList();

        if (nearLat == null || nearLng == null) {
            return mapped;
        }
        return rankByProximity(mapped, nearLat, nearLng, finalLimit);
    }

    // Un "Iguala" en Tabasco a 700km no le sirve a nadie buscando en Guerrero, aunque coincida el
    // texto exacto - si ya hay opciones genuinamente cercanas, las lejanas se quitan de la lista en
    // vez de solo mandarlas al final (verlas ahi igual confunde/resta confianza). Si NINGUNA esta
    // cerca, se dejan todas (puede ser una busqueda real hacia otra ciudad).
    private static final double NEARBY_THRESHOLD_KM = 150;

    private List<GeocodeResult> rankByProximity(List<GeocodeResult> candidates, double nearLat, double nearLng, int finalLimit) {
        record Scored(GeocodeResult result, double distanceKm) {
        }
        List<Scored> scored = candidates.stream()
                .map(r -> new Scored(r, GeoUtils.haversineKm(nearLat, nearLng, r.lat(), r.lng())))
                .sorted(java.util.Comparator.comparingDouble(Scored::distanceKm))
                .toList();
        boolean hasNearbyMatch = scored.stream().anyMatch(s -> s.distanceKm() <= NEARBY_THRESHOLD_KM);
        return scored.stream()
                .filter(s -> !hasNearbyMatch || s.distanceKm() <= NEARBY_THRESHOLD_KM)
                .limit(finalLimit)
                .map(Scored::result)
                .toList();
    }

    @Override
    public String reverseGeocode(double lat, double lng) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/reverse")
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .queryParam("format", "jsonv2")
                .build()
                .encode()
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
