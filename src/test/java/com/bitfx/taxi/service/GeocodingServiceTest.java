package com.bitfx.taxi.service;

import com.bitfx.taxi.service.geocoding.GeocodeResult;
import com.bitfx.taxi.service.geocoding.GeocodingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private GeocodingProvider geocodingProvider;

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        geocodingService = new GeocodingService(geocodingProvider);
    }

    @Test
    void delegaLaGeocodificacionDirectaAlProveedor() {
        GeocodeResult expected = new GeocodeResult(19.4326, -99.1332, "Zocalo, CDMX");
        when(geocodingProvider.forwardGeocode("Zocalo, CDMX")).thenReturn(expected);

        assertEquals(expected, geocodingService.forwardGeocode("Zocalo, CDMX"));
    }

    @Test
    void delegaLaGeocodificacionInversaAlProveedor() {
        when(geocodingProvider.reverseGeocode(19.43, -99.13)).thenReturn("Centro Historico, CDMX");

        assertEquals("Centro Historico, CDMX", geocodingService.reverseGeocode(19.43, -99.13));
    }

    @Test
    void laLlaveDeCacheRedondeaCoordenadasCercanas() {
        String key1 = geocodingService.roundKey(19.43261, -99.13321);
        String key2 = geocodingService.roundKey(19.43263, -99.13322);

        assertEquals(key1, key2);
    }
}
