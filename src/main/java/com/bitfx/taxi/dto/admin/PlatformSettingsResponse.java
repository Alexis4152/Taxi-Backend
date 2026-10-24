package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.FeeType;
import com.bitfx.taxi.model.PlatformSettings;

import java.math.BigDecimal;

public record PlatformSettingsResponse(
        int tripGeofenceRadiusMeters,
        double dispatchSearchRadiusKm,
        String referralRewardDescription,
        String clabe,
        FeeType operatorFeeType,
        BigDecimal operatorFeeValue,
        FeeType passengerFeeType,
        BigDecimal passengerFeeValue,
        int paymentDeadlineHour,
        int paymentTripThreshold,
        boolean fareRoundToMultipleOf5,
        boolean referralProgramEnabled
) {
    public static PlatformSettingsResponse from(PlatformSettings s) {
        return new PlatformSettingsResponse(
                s.getTripGeofenceRadiusMeters(),
                s.getDispatchSearchRadiusKm(),
                s.getReferralRewardDescription(),
                s.getClabe(),
                s.getOperatorFeeType(),
                s.getOperatorFeeValue(),
                s.getPassengerFeeType(),
                s.getPassengerFeeValue(),
                s.getPaymentDeadlineHour(),
                s.getPaymentTripThreshold(),
                s.isFareRoundToMultipleOf5(),
                s.isReferralProgramEnabled()
        );
    }
}
