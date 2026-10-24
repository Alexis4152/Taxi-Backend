package com.bitfx.taxi.dto.passenger;

import com.bitfx.taxi.model.TrustedContact;

public record TrustedContactResponse(Long id, String name, String phone) {
    public static TrustedContactResponse from(TrustedContact contact) {
        return new TrustedContactResponse(contact.getId(), contact.getName(), contact.getPhone());
    }
}
