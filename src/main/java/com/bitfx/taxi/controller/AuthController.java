package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import com.bitfx.taxi.dto.auth.*;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * El refresh token viaja en el cuerpo de la peticion (lo guarda el frontend en sessionStorage,
 * aislado por pestaña), no en una cookie httpOnly compartida por todo el navegador. Se eligio asi
 * a proposito: con una cookie de navegador, abrir pasajero/operador/admin en pestañas distintas de
 * un mismo navegador hacia que cada login pisara la sesion de las demas pestañas (una cookie no
 * puede ser "por pestaña"). El costo de este cambio es que el token ya no esta protegido de
 * JavaScript por httpOnly (ver sessionStorage en el frontend) - se acepto ese tradeoff a cambio de
 * sesiones independientes de verdad por pestaña.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterPassengerRequest req) {
        var result = authService.registerPassenger(req);
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.refreshToken(), result.user())));
    }

    @PostMapping("/register-driver")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(@Valid @RequestBody RegisterDriverRequest req) {
        var result = authService.registerDriver(req);
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.refreshToken(), result.user())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        var result = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), result.refreshToken(), result.user())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshRequest req) {
        var result = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(result.accessToken(), req.refreshToken(), result.user())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshRequest req) {
        authService.logout(req != null ? req.refreshToken() : null);
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
}
