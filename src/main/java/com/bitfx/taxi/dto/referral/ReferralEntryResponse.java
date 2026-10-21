package com.bitfx.taxi.dto.referral;

import com.bitfx.taxi.model.User;

import java.time.LocalDateTime;

public record ReferralEntryResponse(
        String referrerName,
        String referrerPhone,
        String referredName,
        String referredPhone,
        String referredRole,
        LocalDateTime referredAt
) {
    public static ReferralEntryResponse from(User referred) {
        User referrer = referred.getReferredBy();
        return new ReferralEntryResponse(
                referrer.getName(),
                referrer.getPhone(),
                referred.getName(),
                referred.getPhone(),
                referred.getRole().name(),
                referred.getCreatedAt()
        );
    }
}
