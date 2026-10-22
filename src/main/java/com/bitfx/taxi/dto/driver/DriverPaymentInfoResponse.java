package com.bitfx.taxi.dto.driver;

import com.bitfx.taxi.model.FeeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverPaymentInfoResponse(
        BigDecimal pendingAmount,
        int unpaidTripCount,
        int tripThreshold,
        LocalDateTime paymentDueAt,
        boolean paymentBlocked,
        String clabe,
        FeeType operatorFeeType,
        BigDecimal operatorFeeValue,
        FeeType passengerFeeType,
        BigDecimal passengerFeeValue
) {
}
