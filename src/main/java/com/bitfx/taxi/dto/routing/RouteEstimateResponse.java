package com.bitfx.taxi.dto.routing;

import java.math.BigDecimal;
import java.util.List;

public record RouteEstimateResponse(
        BigDecimal distanceKm,
        BigDecimal durationMin,
        BigDecimal estimatedFare,
        BigDecimal passengerServiceFee,
        List<List<Double>> routeGeometry
) {
}
