package com.bitfx.taxi.service;

import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.PlatformSettings;
import com.bitfx.taxi.model.TariffRule;
import com.bitfx.taxi.repository.TariffRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FareService {

    private static final BigDecimal ROUND_MULTIPLE = BigDecimal.valueOf(5);

    private final TariffRuleRepository tariffRuleRepository;
    private final PlatformSettingsService platformSettingsService;

    public BigDecimal estimateFare(Organization organization, BigDecimal distanceKm) {
        TariffRule tariff = resolveTariff(organization);
        BigDecimal fare = tariff.getBaseFare().add(tariff.getPerKm().multiply(distanceKm));
        if (fare.compareTo(tariff.getMinFare()) < 0) {
            fare = tariff.getMinFare();
        }
        fare = fare.setScale(2, RoundingMode.HALF_UP);
        if (platformSettingsService.get().isFareRoundToMultipleOf5()) {
            fare = roundToMultipleOf5(fare);
        }
        return fare;
    }

    // Redondea al multiplo de 5 mas cercano (42.27 -> 40, 43.80 -> 45) para que el pasajero pueda
    // pagar en efectivo sin necesitar monedas - ver PlatformSettings.fareRoundToMultipleOf5.
    private BigDecimal roundToMultipleOf5(BigDecimal fare) {
        return fare.divide(ROUND_MULTIPLE, 0, RoundingMode.HALF_UP)
                .multiply(ROUND_MULTIPLE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public TariffRule resolveTariff(Organization organization) {
        if (organization != null) {
            var orgTariff = tariffRuleRepository.findByOrganization(organization);
            if (orgTariff.isPresent()) {
                return orgTariff.get();
            }
        }
        return tariffRuleRepository.findByOrganizationIsNull()
                .orElseThrow(() -> new IllegalStateException("No hay una tarifa por defecto configurada"));
    }
}
