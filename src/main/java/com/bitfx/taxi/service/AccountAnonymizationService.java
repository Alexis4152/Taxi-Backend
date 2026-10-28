package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverLocationRepository;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.PasswordResetTokenRepository;
import com.bitfx.taxi.repository.RatingRepository;
import com.bitfx.taxi.repository.RefreshTokenRepository;
import com.bitfx.taxi.repository.SavedPlaceRepository;
import com.bitfx.taxi.repository.TripMessageRepository;
import com.bitfx.taxi.repository.TripRepository;
import com.bitfx.taxi.repository.TrustedContactRepository;
import com.bitfx.taxi.repository.UserRepository;
import com.bitfx.taxi.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Derecho de cancelacion (ARCO): al aprobarse la eliminacion de una cuenta, sus datos personales
 * se anonimizan en vez de borrar las filas. Los viajes y cobros se conservan porque la ley obliga a
 * guardarlos (fiscal, aclaraciones, seguridad), pero ya sin nada que identifique a la persona:
 * nombre, telefono, correo, foto, cuenta bancaria, domicilio, ubicaciones exactas, lugares
 * guardados, contactos de confianza, mensajes y comentarios. Los reportes de incidentes de
 * seguridad se conservan intactos como excepcion legal (defensa ante reclamaciones/autoridades).
 *
 * Antes se intentaba un borrado fisico que la base de datos rechazaba siempre que la cuenta tuviera
 * algun viaje, dejando los datos personales guardados indefinidamente.
 */
@Service
@RequiredArgsConstructor
public class AccountAnonymizationService {

    static final String ANONYMIZED_NAME = "Usuario eliminado";
    static final String ANONYMIZED_ADDRESS = "Ubicacion anonimizada";
    static final String REDACTED_MESSAGE = "[Mensaje eliminado por solicitud del titular]";
    private static final String ANONYMIZED_REASON = "Cuenta eliminada a solicitud del titular";

    private static final List<TripStatus> OPEN_TRIP_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.SEARCHING, TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final TripRepository tripRepository;
    private final TripMessageRepository tripMessageRepository;
    private final RatingRepository ratingRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ArcoRequestService arcoRequestService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void anonymizePassenger(PassengerProfile profile, User actingAdmin) {
        User user = profile.getUser();
        ensureNotAnonymized(user);
        if (tripRepository.findFirstByPassengerAndStatusIn(user, OPEN_TRIP_STATUSES).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El usuario tiene un viaje activo o programado; espera a que termine o se cancele antes de eliminarlo.");
        }

        for (Trip trip : tripRepository.findByPassengerOrderByRequestedAtDesc(user)) {
            anonymizeTripLocations(trip);
        }
        savedPlaceRepository.deleteByPassenger(user);
        trustedContactRepository.deleteByPassenger(user);

        fileStorageService.delete(profile.getPhotoUrl());
        profile.setPhotoUrl(null);
        profile.setPendingDeletion(false);
        profile.setDeactivationReason(ANONYMIZED_REASON);
        passengerProfileRepository.save(profile);

        anonymizeUser(user, actingAdmin);
    }

    @Transactional
    public void anonymizeDriver(DriverProfile driver, User actingAdmin) {
        User user = driver.getUser();
        ensureNotAnonymized(user);
        if (tripRepository.findFirstByDriverAndStatusIn(driver, OPEN_TRIP_STATUSES).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El operador tiene un viaje activo; espera a que termine antes de eliminarlo.");
        }

        // La ubicacion en vivo del operador es dato personal y ya no sirve para nada.
        driverLocationRepository.findByDriver(driver).ifPresent(driverLocationRepository::delete);

        fileStorageService.delete(driver.getPhotoUrl());
        driver.setPhotoUrl(null);
        driver.setBankAccount(null);
        driver.setAddress(null);
        driver.setTaxi(null);
        driver.setActive(false);
        driver.setPendingDeletion(false);
        driver.setDeactivationReason(ANONYMIZED_REASON);
        driverProfileRepository.save(driver);

        anonymizeUser(user, actingAdmin);
    }

    private void anonymizeUser(User user, User actingAdmin) {
        // Primero se cierran sus solicitudes de cancelacion: todavia tiene correo para avisarle.
        arcoRequestService.closeOpenCancellations(user, actingAdmin);

        tripMessageRepository.redactBySender(user, REDACTED_MESSAGE);
        ratingRepository.clearCommentsFrom(user);
        refreshTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);

        user.setName(ANONYMIZED_NAME);
        // El telefono es unico y obligatorio: se reemplaza por un valor que no es un telefono real
        // (libera el numero por si la persona quiere registrarse de nuevo en el futuro).
        user.setPhone("ELIM" + user.getId());
        user.setEmail(null);
        user.setPasswordHash(passwordEncoder.encode(TokenGenerator.generate()));
        user.setLastKnownLat(null);
        user.setLastKnownLng(null);
        user.setMarketingOptIn(false);
        user.setActive(false);
        user.setSessionVersion(user.getSessionVersion() + 1);
        user.setAnonymizedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Se conserva la zona aproximada (~1 km) para estadisticas y aclaraciones, pero no la
    // direccion exacta (domicilio, trabajo) ni la ruta recorrida.
    private void anonymizeTripLocations(Trip trip) {
        trip.setOriginAddress(ANONYMIZED_ADDRESS);
        trip.setDestinationAddress(ANONYMIZED_ADDRESS);
        trip.setOriginLat(roundCoordinate(trip.getOriginLat()));
        trip.setOriginLng(roundCoordinate(trip.getOriginLng()));
        trip.setDestinationLat(roundCoordinate(trip.getDestinationLat()));
        trip.setDestinationLng(roundCoordinate(trip.getDestinationLng()));
        trip.setRouteGeometryJson(null);
        trip.setShareToken(null);
        trip.setSpecialComments(null);
        trip.setVerificationCode(null);
        tripRepository.save(trip);
    }

    private static double roundCoordinate(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void ensureNotAnonymized(User user) {
        if (user.getAnonymizedAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta cuenta ya fue eliminada");
        }
    }
}
