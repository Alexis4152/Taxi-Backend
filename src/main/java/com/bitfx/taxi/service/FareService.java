package com.bitfx.taxi.service;

import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.TariffRule;
import com.bitfx.taxi.repository.TariffRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FareService {

    private final TariffRuleRepository tariffRuleRepository;

    public BigDecimal estimateFare(Organization organization, BigDecimal distanceKm) {
        TariffRule tariff = resolveTariff(organization);
        BigDecimal fare = tariff.getBaseFare().add(tariff.getPerKm().multiply(distanceKm));
        if (fare.compareTo(tariff.getMinFare()) < 0) {
            fare = tariff.getMinFare();
        }
        return fare.setScale(2, RoundingMode.HALF_UP);
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
