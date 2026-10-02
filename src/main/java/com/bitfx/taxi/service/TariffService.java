package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.TariffRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.TariffRule;
import com.bitfx.taxi.repository.OrganizationRepository;
import com.bitfx.taxi.repository.TariffRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRuleRepository tariffRuleRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public TariffRule upsert(TariffRequest req, Organization scopedOrg) {
        Organization org = scopedOrg;
        if (org == null && req.organizationId() != null) {
            org = organizationRepository.findById(req.organizationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
        }
        Organization finalOrg = org;
        TariffRule tariff = (finalOrg != null
                ? tariffRuleRepository.findByOrganization(finalOrg)
                : tariffRuleRepository.findByOrganizationIsNull())
                .orElseGet(() -> TariffRule.builder().organization(finalOrg).build());

        tariff.setBaseFare(req.baseFare());
        tariff.setPerKm(req.perKm());
        tariff.setMinFare(req.minFare());
        return tariffRuleRepository.save(tariff);
    }
}
