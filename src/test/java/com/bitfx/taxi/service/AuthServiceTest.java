package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.auth.ForgotPasswordRequest;
import com.bitfx.taxi.dto.auth.LoginRequest;
import com.bitfx.taxi.dto.auth.RegisterPassengerRequest;
import com.bitfx.taxi.dto.auth.ResetPasswordRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.*;
import com.bitfx.taxi.security.JwtTokenProvider;
import com.bitfx.taxi.security.LoginAttemptService;
import com.bitfx.taxi.util.TokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PassengerProfileRepository passengerProfileRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private DriverLocationRepository driverLocationRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TaxiRepository taxiRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;
    @Mock private OrganizationService organizationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passengerProfileRepository, driverProfileRepository, driverLocationRepository,
                organizationRepository, taxiRepository, refreshTokenRepository, passwordResetTokenRepository,
                passwordEncoder, jwtTokenProvider, new LoginAttemptService(), emailService, organizationService
        );
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 28800000L);
        lenientDefaults();
    }

    private void lenientDefaults() {
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("fake-jwt");
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registraUnPasajeroNuevoConExito() {
        when(userRepository.existsByPhone("5512345678")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");

        var req = new RegisterPassengerRequest("Juan Perez", "5512345678", "Password123!", null, true);
        var result = authService.registerPassenger(req);

        assertNotNull(result.accessToken());
        assertEquals("Juan Perez", result.user().name());
        assertEquals(Role.PASSENGER, result.user().role());
        verify(passengerProfileRepository).save(any(PassengerProfile.class));
    }

    @Test
    void noPermiteRegistrarUnTelefonoYaUsado() {
        when(userRepository.existsByPhone("5512345678")).thenReturn(true);

        var req = new RegisterPassengerRequest("Juan Perez", "5512345678", "Password123!", null, true);

        assertThrows(ApiException.class, () -> authService.registerPassenger(req));
    }

    @Test
    void rechazaLoginConContrasenaIncorrecta() {
        User user = User.builder().id(1L).phone("5512345678").passwordHash("hashed").role(Role.PASSENGER).active(true).build();
        when(userRepository.findByPhone("5512345678")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        var req = new LoginRequest("5512345678", "wrong");

        assertThrows(ApiException.class, () -> authService.login(req));
    }

    @Test
    void bloqueaElLoginTrasVariosIntentosFallidosSeguidos() {
        User user = User.builder().id(1L).phone("5512345678").passwordHash("hashed").role(Role.PASSENGER).active(true).build();
        when(userRepository.findByPhone("5512345678")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        var req = new LoginRequest("5512345678", "wrong");
        for (int i = 0; i < 5; i++) {
            assertThrows(ApiException.class, () -> authService.login(req));
        }

        // Al sexto intento (aunque la contrasena fuera correcta) debe seguir bloqueado por el limite de intentos.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("intentos"));
    }

    @Test
    void unLoginUnicoFuncionaParaCualquierRolSinPedirDatosExtra() {
        Organization org = Organization.builder().id(1L).name("Demo").build();
        User driverUser = User.builder().id(2L).phone("5599990000").passwordHash("hashed").role(Role.DRIVER).organization(org).active(true).build();
        DriverProfile driverProfile = DriverProfile.builder().id(10L).user(driverUser).organization(org).active(true).build();

        when(userRepository.findByPhone("5599990000")).thenReturn(Optional.of(driverUser));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);
        when(driverProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(driverProfile));

        var req = new LoginRequest("5599990000", "Password123!");
        var result = authService.login(req);

        assertEquals(Role.DRIVER, result.user().role());
    }

    @Test
    void unOperadorDesactivadoNoPuedeIniciarSesion() {
        Organization org = Organization.builder().id(1L).name("Demo").build();
        User driverUser = User.builder().id(2L).phone("5599990000").passwordHash("hashed").role(Role.DRIVER).organization(org).active(true).build();
        DriverProfile driverProfile = DriverProfile.builder().id(10L).user(driverUser).organization(org).active(false).build();

        when(userRepository.findByPhone("5599990000")).thenReturn(Optional.of(driverUser));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);
        when(driverProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(driverProfile));

        var req = new LoginRequest("5599990000", "Password123!");

        assertThrows(ApiException.class, () -> authService.login(req));
    }

    @Test
    void resumenDeSesionIncluyeElTaxiActualDelOperadorSiTieneUnoAsignado() {
        Organization org = Organization.builder().id(1L).name("Demo").build();
        User driverUser = User.builder().id(2L).phone("5599990000").passwordHash("hashed").role(Role.DRIVER).organization(org).active(true).build();
        Taxi taxi = Taxi.builder().id(100L).organization(org).unitNumber("007").plates("XYZ-1").active(true).build();
        DriverProfile driverProfile = DriverProfile.builder().id(10L).user(driverUser).organization(org).taxi(taxi).active(true).build();

        when(userRepository.findByPhone("5599990000")).thenReturn(Optional.of(driverUser));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);
        when(driverProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(driverProfile));
        when(driverProfileRepository.findByTaxi(taxi)).thenReturn(java.util.List.of(driverProfile));

        var result = authService.login(new LoginRequest("5599990000", "Password123!"));

        assertNotNull(result.user().currentTaxi());
        assertEquals("007", result.user().currentTaxi().unitNumber());
    }

    @Test
    void forgotPasswordNoRevelaSiElCorreoExisteONo() {
        when(userRepository.findAllByEmailIgnoreCase("nadie@ejemplo.com")).thenReturn(java.util.List.of());

        // No debe lanzar ninguna excepcion ni distinguirse de una cuenta que si existe.
        authService.forgotPassword(new ForgotPasswordRequest("nadie@ejemplo.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void forgotPasswordGuardaElTokenHasheadoYEnviaElTokenCrudoPorCorreo() {
        User user = User.builder().id(1L).phone("5512345678").email("juan@ejemplo.com").passwordHash("hashed").role(Role.PASSENGER).active(true).build();
        when(userRepository.findAllByEmailIgnoreCase("juan@ejemplo.com")).thenReturn(java.util.List.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest("juan@ejemplo.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("juan@ejemplo.com"), rawTokenCaptor.capture());

        String rawTokenSentByEmail = rawTokenCaptor.getValue();
        String storedToken = tokenCaptor.getValue().getToken();
        assertNotEquals(rawTokenSentByEmail, storedToken, "El token guardado en BD no debe ser el mismo valor en claro que se envia por correo");
        assertEquals(TokenGenerator.hashToken(rawTokenSentByEmail), storedToken, "El token guardado debe ser el hash del token enviado");
    }

    @Test
    void forgotPasswordFuncionaAunSiDosCuentasComparenElMismoCorreo() {
        User admin = User.builder().id(1L).phone("5510000001").email("compartido@ejemplo.com").passwordHash("hashed").role(Role.ADMIN).active(true).build();
        User passenger = User.builder().id(2L).phone("5510000002").email("compartido@ejemplo.com").passwordHash("hashed").role(Role.PASSENGER).active(true).build();
        when(userRepository.findAllByEmailIgnoreCase("compartido@ejemplo.com")).thenReturn(java.util.List.of(admin, passenger));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // No debe lanzar IncorrectResultSizeDataAccessException ni ningun otro error: debe generar
        // un enlace independiente para cada cuenta que use ese correo.
        authService.forgotPassword(new ForgotPasswordRequest("compartido@ejemplo.com"));

        verify(passwordResetTokenRepository, times(2)).save(any(PasswordResetToken.class));
        verify(emailService, times(2)).sendPasswordResetEmail(eq("compartido@ejemplo.com"), any());
    }

    @Test
    void forgotPasswordSeBloqueaTrasVariasSolicitudesSeguidasParaElMismoCorreo() {
        when(userRepository.findAllByEmailIgnoreCase("juan@ejemplo.com")).thenReturn(java.util.List.of());
        var req = new ForgotPasswordRequest("juan@ejemplo.com");

        for (int i = 0; i < 5; i++) {
            authService.forgotPassword(req);
        }

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.forgotPassword(req));
        assertTrue(ex.getMessage().toLowerCase().contains("intentos"));
    }

    @Test
    void resetPasswordConTokenValidoActualizaLaContrasenaYRevocaSesiones() {
        User user = User.builder().id(1L).phone("5512345678").passwordHash("old-hash").role(Role.PASSENGER).mustChangePassword(true).active(true).build();
        String rawToken = "un-token-de-prueba";
        PasswordResetToken resetToken = PasswordResetToken.builder().id(1L).user(user)
                .token(TokenGenerator.hashToken(rawToken)).expiresAt(LocalDateTime.now().plusMinutes(10)).used(false).build();
        when(passwordResetTokenRepository.findByToken(TokenGenerator.hashToken(rawToken))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NuevaPassword123!")).thenReturn("new-hash");
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));
        RefreshToken activeSession = RefreshToken.builder().id(1L).user(user).token("rt").revoked(false).expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(refreshTokenRepository.findByUserAndRevokedFalse(user)).thenReturn(java.util.List.of(activeSession));

        authService.resetPassword(new ResetPasswordRequest(rawToken, "NuevaPassword123!"));

        assertEquals("new-hash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        assertTrue(resetToken.isUsed());
        assertTrue(activeSession.isRevoked(), "Las sesiones activas deben revocarse al restablecer la contrasena");
    }

    @Test
    void resetPasswordRechazaUnTokenQueNoExiste() {
        when(passwordResetTokenRepository.findByToken(any())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> authService.resetPassword(new ResetPasswordRequest("token-inventado", "NuevaPassword123!")));
    }

    @Test
    void resetPasswordRechazaUnTokenYaUsado() {
        User user = User.builder().id(1L).phone("5512345678").passwordHash("old-hash").role(Role.PASSENGER).active(true).build();
        String rawToken = "otro-token";
        PasswordResetToken resetToken = PasswordResetToken.builder().id(2L).user(user)
                .token(TokenGenerator.hashToken(rawToken)).expiresAt(LocalDateTime.now().plusMinutes(10)).used(true).build();
        when(passwordResetTokenRepository.findByToken(TokenGenerator.hashToken(rawToken))).thenReturn(Optional.of(resetToken));

        assertThrows(ApiException.class, () -> authService.resetPassword(new ResetPasswordRequest(rawToken, "NuevaPassword123!")));
    }

    @Test
    void resetPasswordRechazaUnTokenExpirado() {
        User user = User.builder().id(1L).phone("5512345678").passwordHash("old-hash").role(Role.PASSENGER).active(true).build();
        String rawToken = "token-expirado";
        PasswordResetToken resetToken = PasswordResetToken.builder().id(3L).user(user)
                .token(TokenGenerator.hashToken(rawToken)).expiresAt(LocalDateTime.now().minusMinutes(1)).used(false).build();
        when(passwordResetTokenRepository.findByToken(TokenGenerator.hashToken(rawToken))).thenReturn(Optional.of(resetToken));

        assertThrows(ApiException.class, () -> authService.resetPassword(new ResetPasswordRequest(rawToken, "NuevaPassword123!")));
    }
}
