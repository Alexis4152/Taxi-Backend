package com.bitfx.taxi.service.route;

public interface RouteProvider {
    RouteEstimate estimate(double originLat, double originLng, double destinationLat, double destinationLng);
}
