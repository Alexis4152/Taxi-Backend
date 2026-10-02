package com.bitfx.taxi.service;

import com.bitfx.taxi.model.TariffRule;
import com.bitfx.taxi.repository.TariffRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private TariffRuleRepository tariffRuleRepository;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareService(tariffRuleRepository);
    }

    @Test
    void calculaTarifaBasePorDistanciaCuandoSuperaLaMinima() {
        TariffRule defaultTariff = TariffRule.builder()
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("8.00"))
                .minFare(new BigDecimal("30.00"))
                .build();
        when(tariffRuleRepository.findByOrganizationIsNull()).thenReturn(Optional.of(defaultTariff));

        BigDecimal fare = fareService.estimateFare(null, new BigDecimal("5.00"));

        // 15 + 8*5 = 55.00, por encima de la tarifa minima de 30.00
        assertEquals(new BigDecimal("55.00"), fare);
    }

    @Test
    void aplicaTarifaMinimaCuandoElCalculoQuedaPorDebajo() {
        TariffRule defaultTariff = TariffRule.builder()
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("8.00"))
                .minFare(new BigDecimal("30.00"))
                .build();
        when(tariffRuleRepository.findByOrganizationIsNull()).thenReturn(Optional.of(defaultTariff));

        BigDecimal fare = fareService.estimateFare(null, new BigDecimal("0.50"));

        // 15 + 8*0.5 = 19.00, por debajo del minimo de 30.00 -> debe cobrar la minima
        assertEquals(new BigDecimal("30.00"), fare);
    }
}
