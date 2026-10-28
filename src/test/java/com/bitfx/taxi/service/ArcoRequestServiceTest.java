package com.bitfx.taxi.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcoRequestServiceTest {

    @Test
    void elPlazoDe20DiasHabilesSaltaFinesDeSemana() {
        // Lunes 2 de noviembre de 2026 -> 20 dias habiles = 4 semanas exactas -> lunes 30 de noviembre
        LocalDateTime monday = LocalDateTime.of(2026, 11, 2, 10, 0);
        assertEquals(LocalDateTime.of(2026, 11, 30, 10, 0), ArcoRequestService.addBusinessDays(monday, 20));
    }

    @Test
    void unaSolicitudRecibidaEnSabadoEmpiezaAContarElLunes() {
        LocalDateTime saturday = LocalDateTime.of(2026, 10, 31, 9, 0);
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 0), ArcoRequestService.addBusinessDays(saturday, 1));
    }
}
