package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.auth.*;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.*;
import com.bitfx.taxi.security.JwtTokenProvider;
import com.bitfx.taxi.security.LoginAttemptService;
import com.bitfx.taxi.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final OrganizationRepository organizationRepository;
    private final TaxiRepository taxiRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    private final OrganizationService organizationService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResult registerPassenger(RegisterPassengerRequest req) {
        if (userRepository.existsByPhone(req.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese telefono");
        }
        User user = User.builder()
                .phone(req.phone())
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.PASSENGER)
                .active(true)
                .termsAcceptedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        PassengerProfile profile = PassengerProfile.builder().user(user).build();
        passengerProfileRepository.save(profile);

        return buildAuthResult(user);
    }

    /**
     * Autoregistro de operador: cualquier persona con su propio taxi puede darse de alta sin que
     * un admin tenga que crear su cuenta. El taxi se busca por placas (si ya existe, varios
     * operadores pueden compartirlo - lo rotan ellos mismos); si no existe, se crea.
     */
    @Transactional
    public AuthResult registerDriver(RegisterDriverRequest req) {
        if (userRepository.existsByPhone(req.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese telefono");
        }
        Organization org = req.organizationId() != null
                ? organizationRepository.findById(req.organizationId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"))
                : organizationService.resolveIndependentOrganization();

        Taxi taxi = taxiRepository.findByPlates(req.taxiPlates().trim().toUpperCase()).orElseGet(() -> taxiRepository.save(
                Taxi.builder()
                        .organization(org)
                        .unitNumber(req.taxiUnitNumber())
                        .plates(req.taxiPlates().trim().toUpperCase())
                        .brand(req.taxiBrand())
                        .model(req.taxiModel())
                        .active(true)
                        .build()));

        User user = User.builder()
                .phone(req.phone())
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.DRIVER)
                .organization(taxi.getOrganization())
                .active(true)
                .termsAcceptedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .organization(taxi.getOrganization())
                .taxi(taxi)
                .active(true)
                .build();
        driverProfileRepository.save(profile);

        emailService.sendDriverSelfRegisteredEmail(req.email(), req.name(), taxi.getUnitNumber(), taxi.getPlates());

        return buildAuthResult(user);
    }

    /**
     * Login unico para cualquier rol (pasajero, operador, admin, super admin): el sistema
     * determina el rol y sus privilegios a partir de lo que ya esta en base de datos, sin que el
     * usuario tenga que elegir un tipo de cuenta ni (si es operador) indicar un numero de taxi -
     * ese dato se resuelve solo a partir del taxi que trae vinculado (ver toSummary/currentTaxi).
     */
    @Transactional
    public AuthResult login(LoginRequest req) {
        String attemptKey = "login:" + req.phone();
        loginAttemptService.checkNotLocked(attemptKey);

        User user = userRepository.findByPhone(req.phone()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttemptService.onFailure(attemptKey);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Telefono o contrasena incorrectos");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta esta desactivada, contacta al administrador");
        }
        if (user.getRole() == Role.DRIVER) {
            DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId()).orElse(null);
            if (driver == null || !driver.isActive()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Tu perfil de operador esta desactivado, contacta al administrador");
            }
            // Un mismo taxi puede tener varios operadores registrados (lo rotan ellos mismos), pero
            // solo uno puede estar conectado a la vez: si otro ya esta en linea con este taxi, no
            // se le deja entrar hasta que ese se desconecte.
            if (driver.getTaxi() != null) {
                boolean occupiedByOther = driverProfileRepository.findByTaxi(driver.getTaxi()).stream()
                        .filter(other -> !other.getId().equals(driver.getId()))
                        .anyMatch(other -> driverLocationRepository.findByDriver(other).map(DriverLocation::isOnline).orElse(false));
                if (occupiedByOther) {
                    throw new ApiException(HttpStatus.FORBIDDEN,
                            "Ya hay un operador usando este taxi, espera a que se desconecte o contacta al administrador.");
                }
            }
        }
        loginAttemptService.onSuccess(attemptKey);
        return buildAuthResult(user);
    }

    @Transactional(readOnly = true)
    public RefreshResult refresh(String refreshTokenValue) {
        if (refreshTokenValue == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sesion invalida, vuelve a iniciar sesion");
        }
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesion invalida, vuelve a iniciar sesion"));
        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesion expiro, vuelve a iniciar sesion");
        }
        User user = token.getUser();
        String accessToken = jwtTokenProvider.generateToken(user.getPhone(), user.getId(), user.getRole().name());
        return new RefreshResult(accessToken, toSummary(user));
    }

    public UserSummary toSummary(User user) {
        if (user.getRole() != Role.DRIVER) {
            return UserSummary.from(user, null, null);
        }
        DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId()).orElse(null);
        if (driver == null) {
            return UserSummary.from(user, null, null);
        }
        CurrentTaxiSummary currentTaxi = driver.getTaxi() != null
                ? new CurrentTaxiSummary(driver.getTaxi().getId(), driver.getTaxi().getUnitNumber(), driver.getTaxi().getPlates())
                : null;
        return UserSummary.from(user, driver.getId(), currentTaxi);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null) {
            return;
        }
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        String normalizedEmail = req.email().trim().toLowerCase();
        // Limite de solicitudes por correo, independiente de si la cuenta existe: evita que se use
        // este endpoint para bombardear de correos a una direccion o para tantear cuentas en bulk.
        String attemptKey = "forgot:" + normalizedEmail;
        loginAttemptService.checkNotLocked(attemptKey);
        loginAttemptService.onFailure(attemptKey);

        // Un mismo correo puede estar en mas de una cuenta (ej. operador y pasajero con telefonos
        // distintos); se genera y envia un enlace independiente para cada una.
        userRepository.findAllByEmailIgnoreCase(normalizedEmail).forEach(user -> {
            String rawToken = TokenGenerator.generate();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(TokenGenerator.hashToken(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
        // Respuesta identica exista o no la cuenta, para no filtrar que correos estan registrados.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(TokenGenerator.hashToken(req.token()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El enlace de recuperacion es invalido"));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace de recuperacion expiro o ya fue usado");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(t -> t.setRevoked(true));
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contrasena actual es incorrecta");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(t -> t.setRevoked(true));
    }

    private AuthResult buildAuthResult(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getPhone(), user.getId(), user.getRole().name());
        String refreshToken = issueRefreshToken(user);
        return new AuthResult(accessToken, refreshToken, toSummary(user));
    }

    private String issueRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(TokenGenerator.generate())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        refreshTokenRepository.save(token);
        return token.getToken();
    }

    public record RefreshResult(String accessToken, UserSummary user) {
    }

    public record AuthResult(String accessToken, String refreshToken, UserSummary user) {
    }
}
