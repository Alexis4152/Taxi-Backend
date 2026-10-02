package com.bitfx.taxi.dto.trip;

import java.math.BigDecimal;

public record DriverStatsDto(
        long tripsCompletedToday,
        long tripsAcceptedToday,
        long tripsCancelledByPassengerToday,
        BigDecimal earningsToday,
        BigDecimal tipsToday
) {
}
