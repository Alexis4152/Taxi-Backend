package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.auth.*;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "taxi_refresh_token";

    private final AuthService authService;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.samesite}")
    private String cookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterPassengerRequest req,
                                                               HttpServletResponse response) {
        var result = authService.registerPassenger(req);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.user())));
    }

    @PostMapping("/register-driver")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(@Valid @RequestBody RegisterDriverRequest req,
                                                                     HttpServletResponse response) {
        var result = authService.registerDriver(req);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.user())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req,
                                                            HttpServletResponse response) {
        var result = authService.login(req);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.user())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        var result = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.user())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                      HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Si el correo esta registrado, recibiras un enlace para recuperar tu contrasena", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada correctamente", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                                              Authentication authentication) {
        User user = currentUser(authentication);
        authService.changePassword(user, req);
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada correctamente", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummary>> me(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(ApiResponse.ok(authService.toSummary(user)));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AuthenticationCredentialsNotFoundException("No autenticado");
        }
        return user;
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(60L * 60 * 8)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
