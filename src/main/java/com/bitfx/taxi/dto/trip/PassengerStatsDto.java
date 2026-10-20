package com.bitfx.taxi.dto.trip;

public record PassengerStatsDto(
        long totalTrips,
        long tripsCompletedToday,
        long tripsCancelledByDriverToday,
        long tripsCancelledByMeToday
) {
}
