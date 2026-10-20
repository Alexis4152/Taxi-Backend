package com.bitfx.taxi.dto.auth;

public record AuthResponse(String accessToken, String refreshToken, UserSummary user) {
}
