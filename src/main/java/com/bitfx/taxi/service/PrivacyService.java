package com.bitfx.taxi.service;

import com.bitfx.taxi.model.Role;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.RatingRepository;
import com.bitfx.taxi.repository.SavedPlaceRepository;
import com.bitfx.taxi.repository.TripMessageRepository;
import com.bitfx.taxi.repository.TrustedContactRepository;
import com.bitfx.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Autoservicio de privacidad del titular: descarga de sus datos (derecho de acceso), aceptacion
 * del aviso de privacidad y consentimiento para finalidades secundarias (promociones).
 */
@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final UserRepository userRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final TrustedContactRepository trustedContactRepository;
    private final TripMessageRepository tripMessageRepository;
    private final RatingRepository ratingRepository;
    private final TripService tripService;
    private final RatingService ratingService;
    private final ArcoRequestService arcoRequestService;

    @Transactional
    public void acceptPrivacyNotice(User user, boolean marketingOptIn) {
        user.setPrivacyAcceptedAt(LocalDateTime.now());
        user.setMarketingOptIn(marketingOptIn);
        userRepository.save(user);
    }

    @Transactional
    public void updatePreferences(User user, boolean marketingOptIn) {
        user.setMarketingOptIn(marketingOptIn);
        userRepository.save(user);
    }

    /**
     * Todo lo que la plataforma guarda de la persona, en un formato legible (JSON). No incluye
     * datos internos sin valor para el titular (hashes de contrasena, tokens de sesion).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportMyData(User user) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("generadoEl", LocalDateTime.now());
        export.put("plataforma", "NexoraTaxis");

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", user.getId());
        account.put("nombre", user.getName());
        account.put("telefono", user.getPhone());
        account.put("correo", user.getEmail());
        account.put("rol", user.getRole());
        account.put("organizacion", user.getOrganization() != null ? user.getOrganization().getName() : null);
        account.put("cuentaCreadaEl", user.getCreatedAt());
        account.put("terminosAceptadosEl", user.getTermsAcceptedAt());
        account.put("avisoPrivacidadAceptadoEl", user.getPrivacyAcceptedAt());
        account.put("aceptaPromociones", user.isMarketingOptIn());
        account.put("codigoDeReferido", user.getReferralCode());
        account.put("ultimaUbicacionConocida", user.getLastKnownLat() != null
                ? Map.of("lat", user.getLastKnownLat(), "lng", user.getLastKnownLng()) : null);
        export.put("cuenta", account);

        if (user.getRole() == Role.PASSENGER) {
            passengerProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
                Map<String, Object> profile = new LinkedHashMap<>();
                profile.put("foto", p.getPhotoUrl());
                profile.put("calificacionPromedio", p.getRatingAvg());
                profile.put("numeroDeCalificaciones", p.getRatingCount());
                export.put("perfilPasajero", profile);
            });
            export.put("lugaresGuardados", savedPlaceRepository.findByPassengerIdOrderByCreatedAtAsc(user.getId()).stream()
                    .map(sp -> Map.of("etiqueta", sp.getLabel(), "direccion", sp.getAddress(), "lat", sp.getLat(), "lng", sp.getLng()))
                    .toList());
            export.put("contactosDeConfianza", trustedContactRepository.findByPassengerIdOrderByCreatedAtAsc(user.getId()).stream()
                    .map(tc -> Map.of("nombre", tc.getName(), "telefono", tc.getPhone()))
                    .toList());
            export.put("viajes", tripService.getMyTripHistoryAsPassenger(user));
        }

        if (user.getRole() == Role.DRIVER) {
            driverProfileRepository.findByUser_Id(user.getId()).ifPresent(d -> {
                Map<String, Object> profile = new LinkedHashMap<>();
                profile.put("foto", d.getPhotoUrl());
                profile.put("domicilio", d.getAddress());
                profile.put("cuentaBancaria", d.getBankAccount());
                profile.put("taxi", d.getTaxi() != null
                        ? "Unidad " + d.getTaxi().getUnitNumber() + " (" + d.getTaxi().getPlates() + ")" : null);
                profile.put("calificacionPromedio", d.getRatingAvg());
                profile.put("numeroDeCalificaciones", d.getRatingCount());
                profile.put("saldoPendiente", d.getPendingAmount());
                export.put("perfilOperador", profile);
            });
            export.put("viajes", tripService.getMyTripHistoryAsDriver(user));
        }

        export.put("calificacionesRecibidas", ratingService.getRatingsForUser(user.getId()));
        export.put("calificacionesHechas", ratingRepository.findByFromUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(r -> {
                    Map<String, Object> rating = new LinkedHashMap<>();
                    rating.put("viajeId", r.getTrip().getId());
                    rating.put("puntuacion", r.getScore());
                    rating.put("comentario", r.getComment());
                    rating.put("fecha", r.getCreatedAt());
                    return rating;
                })
                .toList());
        export.put("mensajesEnviados", tripMessageRepository.findBySenderUserOrderByCreatedAtAsc(user).stream()
                .map(m -> Map.of("viajeId", m.getTrip().getId(), "mensaje", m.getBody(), "fecha", m.getCreatedAt()))
                .toList());
        export.put("solicitudesArco", arcoRequestService.listMine(user));
        return export;
    }
}
