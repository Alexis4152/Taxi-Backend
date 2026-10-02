package com.bitfx.taxi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

    @Test
    void distanciaEntreElMismoPuntoEsCero() {
        double km = GeoUtils.haversineKm(19.4326, -99.1332, 19.4326, -99.1332);
        assertEquals(0.0, km, 0.0001);
    }

    @Test
    void distanciaEntreZocaloYAngelDeLaIndependenciaEsRazonable() {
        // Zocalo CDMX a el Angel de la Independencia son aproximadamente 3.4 km en linea recta.
        double km = GeoUtils.haversineKm(19.4326, -99.1332, 19.4270, -99.1677);
        assertTrue(km > 3.0 && km < 4.0, "Se esperaba una distancia cercana a 3.4 km, se obtuvo " + km);
    }
}
