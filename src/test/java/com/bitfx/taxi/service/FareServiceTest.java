package com.bitfx.taxi.service;

import com.bitfx.taxi.model.FeeType;
import com.bitfx.taxi.model.PlatformSettings;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private TariffRuleRepository tariffRuleRepository;

    @Mock
    private PlatformSettingsService platformSettingsService;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        lenient().when(platformSettingsService.get()).thenReturn(PlatformSettings.builder().fareRoundToMultipleOf5(false).build());
        fareService = new FareService(tariffRuleRepository, platformSettingsService);
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

    @Test
    void redondeaAlMultiploDe5MasCercanoCuandoEstaActivado() {
        when(platformSettingsService.get()).thenReturn(PlatformSettings.builder().fareRoundToMultipleOf5(true).build());
        TariffRule defaultTariff = TariffRule.builder()
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("8.27"))
                .minFare(new BigDecimal("30.00"))
                .build();
        when(tariffRuleRepository.findByOrganizationIsNull()).thenReturn(Optional.of(defaultTariff));

        // 15 + 8.27*1 = 23.27 -> por debajo del minimo, sube a 30.00 -> ya es multiplo de 5, se queda igual
        assertEquals(new BigDecimal("30.00"), fareService.estimateFare(null, new BigDecimal("1.00")));
        // 15 + 8.27*3.3 = 42.291 -> 42.29, distancia a 40 es 2.29 (< 2.50) -> baja a 40.00
        assertEquals(new BigDecimal("40.00"), fareService.estimateFare(null, new BigDecimal("3.30")));
        // 15 + 8.27*3.48 = 43.7796 -> 43.78, distancia a 40 es 3.78 (>= 2.50) -> sube a 45.00
        assertEquals(new BigDecimal("45.00"), fareService.estimateFare(null, new BigDecimal("3.48")));
    }

    @Test
    void redondeaAlPesoMasCercanoCuandoEstaActivado() {
        when(platformSettingsService.get()).thenReturn(PlatformSettings.builder().fareRoundToWholePeso(true).build());
        TariffRule defaultTariff = TariffRule.builder()
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("10.00"))
                .minFare(new BigDecimal("30.00"))
                .build();
        when(tariffRuleRepository.findByOrganizationIsNull()).thenReturn(Optional.of(defaultTariff));

        // 15 + 10*2.749 = 42.49 -> menos de .50, baja a 42.00
        assertEquals(new BigDecimal("42.00"), fareService.estimateFare(null, new BigDecimal("2.749")));
        // 15 + 10*2.75 = 42.50 -> .50 o mas, sube a 43.00
        assertEquals(new BigDecimal("43.00"), fareService.estimateFare(null, new BigDecimal("2.75")));
    }

    @Test
    void elMultiploDe5TienePrioridadSobreElRedondeoAlPeso() {
        when(platformSettingsService.get()).thenReturn(
                PlatformSettings.builder().fareRoundToMultipleOf5(true).fareRoundToWholePeso(true).build());
        TariffRule defaultTariff = TariffRule.builder()
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("10.00"))
                .minFare(new BigDecimal("30.00"))
                .build();
        when(tariffRuleRepository.findByOrganizationIsNull()).thenReturn(Optional.of(defaultTariff));

        // 42.50 -> multiplo de 5 mas cercano es 45 (no 43)
        assertEquals(new BigDecimal("45.00"), fareService.estimateFare(null, new BigDecimal("2.75")));
    }

    @Test
    void losCobrosDeServicioTambienSeRedondeanAlPeso() {
        PlatformSettings settings = PlatformSettings.builder()
                .passengerFeeType(FeeType.PERCENT).passengerFeeValue(new BigDecimal("7"))
                .operatorFeeType(FeeType.PERCENT).operatorFeeValue(new BigDecimal("9"))
                .fareRoundToWholePeso(true)
                .build();
        // 7% de 43 = 3.01 -> 3.00 ; 9% de 43 = 3.87 -> 4.00
        assertEquals(new BigDecimal("3.00"), PlatformSettingsService.passengerFee(settings, new BigDecimal("43.00")));
        assertEquals(new BigDecimal("4.00"), PlatformSettingsService.operatorFee(settings, new BigDecimal("43.00")));

        settings.setFareRoundToWholePeso(false);
        assertEquals(new BigDecimal("3.01"), PlatformSettingsService.passengerFee(settings, new BigDecimal("43.00")));
    }
}
