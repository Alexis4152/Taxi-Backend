package com.bitfx.taxi.dto.geocoding;

import com.bitfx.taxi.service.geocoding.GeocodeResult;

public record GeocodeSearchResponse(double lat, double lng, String displayName) {
    public static GeocodeSearchResponse from(GeocodeResult r) {
        return new GeocodeSearchResponse(r.lat(), r.lng(), r.displayName());
    }
}
