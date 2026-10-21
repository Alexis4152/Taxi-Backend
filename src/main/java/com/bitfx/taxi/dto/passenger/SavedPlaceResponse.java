package com.bitfx.taxi.dto.passenger;

import com.bitfx.taxi.model.SavedPlace;

public record SavedPlaceResponse(Long id, String label, String address, double lat, double lng) {
    public static SavedPlaceResponse from(SavedPlace place) {
        return new SavedPlaceResponse(place.getId(), place.getLabel(), place.getAddress(), place.getLat(), place.getLng());
    }
}
