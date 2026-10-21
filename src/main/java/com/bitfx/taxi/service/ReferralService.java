package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.referral.MyReferralResponse;
import com.bitfx.taxi.dto.referral.ReferralEntryResponse;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Programa de referidos: cada usuario tiene un codigo propio (ver User.referralCode, generado al
 * registrarse) para invitar a otros. No hay billetera/credito automatico en la app todavia - el
 * "premio" que recibe quien invita es solo texto libre que el superadmin define (ver
 * PlatformSettings.referralRewardDescription) y se aplica manualmente fuera de la app; esto solo
 * trackea quien invito a quien para que el admin pueda honrarlo.
 */
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final UserRepository userRepository;
    private final PlatformSettingsService platformSettingsService;

    @Transactional(readOnly = true)
    public MyReferralResponse getMyReferralInfo(User user) {
        long count = userRepository.countByReferredBy_Id(user.getId());
        String rewardDescription = platformSettingsService.get().getReferralRewardDescription();
        return new MyReferralResponse(user.getReferralCode(), count, rewardDescription);
    }

    @Transactional(readOnly = true)
    public List<ReferralEntryResponse> listAll() {
        return userRepository.findByReferredByIsNotNullOrderByCreatedAtDesc().stream()
                .map(ReferralEntryResponse::from)
                .toList();
    }
}
