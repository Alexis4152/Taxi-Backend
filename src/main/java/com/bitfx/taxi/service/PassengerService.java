package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.privacy.ArcoRequestResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.RefreshTokenRepository;
import com.bitfx.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountAnonymizationService accountAnonymizationService;
    private final ArcoRequestService arcoRequestService;

    public PassengerProfile getByUserId(Long userId) {
        return passengerProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de pasajero"));
    }

    @Transactional
    public PassengerProfile updatePhoto(Long userId, String photoUrl) {
        PassengerProfile profile = passengerProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de pasajero"));
        profile.setPhotoUrl(photoUrl);
        return passengerProfileRepository.save(profile);
    }

    // Las cuentas ya anonimizadas no se listan: no queda nada que administrar en ellas.
    public List<PassengerProfile> listAll() {
        return passengerProfileRepository.findAll().stream()
                .filter(p -> p.getUser().getAnonymizedAt() == null)
                .toList();
    }

    public PassengerProfile getById(Long id) {
        return passengerProfileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pasajero no encontrado"));
    }

    @Transactional
    public void setActive(Long id, boolean active, String reason) {
        PassengerProfile profile = getById(id);
        profile.getUser().setActive(active);
        userRepository.save(profile.getUser());
        // El motivo solo tiene sentido mientras esta bloqueado - se borra solo al reactivar.
        profile.setDeactivationReason(active ? null : reason);
        passengerProfileRepository.save(profile);
        if (!active) {
            invalidateSessions(profile.getUser());
        }
    }

    // Primer paso de "eliminar" (desde el admin): bloquea al usuario y lo marca en revision, sin
    // borrar nada todavia.
    @Transactional
    public void markPendingDeletion(Long id, String reason) {
        PassengerProfile profile = getById(id);
        profile.getUser().setActive(false);
        userRepository.save(profile.getUser());
        profile.setPendingDeletion(true);
        profile.setDeactivationReason(reason);
        passengerProfileRepository.save(profile);
        invalidateSessions(profile.getUser());
    }

    // Bloquear/marcar pendiente de eliminacion no debe dejar utilizable una sesion ya abierta - sin
    // esto, un token valido emitido antes del bloqueo seguia funcionando con normalidad hasta que
    // expirara solo. Misma idea que AuthService.startNewSession, pero disparada al bloquear.
    private void invalidateSessions(User user) {
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(t -> t.setRevoked(true));
    }

    @Transactional
    public void cancelPendingDeletion(Long id) {
        PassengerProfile profile = getById(id);
        profile.setPendingDeletion(false);
        passengerProfileRepository.save(profile);
    }

    // El propio usuario solicita que se elimine su cuenta: se bloquea de inmediato su acceso y
    // queda en revision para que el super admin la acepte (y en ese momento se borre definitivo) o
    // la rechace.
    @Transactional
    public ArcoRequestResponse requestOwnDeletion(User user) {
        PassengerProfile profile = passengerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de pasajero"));
        profile.getUser().setActive(false);
        userRepository.save(profile.getUser());
        profile.setPendingDeletion(true);
        profile.setDeactivationReason("El usuario solicito la eliminacion de su cuenta");
        passengerProfileRepository.save(profile);
        invalidateSessions(profile.getUser());
        return arcoRequestService.registerAccountDeletion(profile.getUser());
    }

    // Eliminacion definitiva - solo permitida si ya paso por markPendingDeletion (admin o el propio
    // usuario). Los datos personales se anonimizan en lugar de borrar las filas, para conservar el
    // historial de viajes que la ley obliga a guardar (ver AccountAnonymizationService).
    @Transactional
    public void deletePermanently(Long id, User actingAdmin) {
        PassengerProfile profile = getById(id);
        if (!profile.isPendingDeletion()) {
            throw new ApiException(HttpStatus.CONFLICT, "Primero debe marcarse como pendiente de eliminacion");
        }
        accountAnonymizationService.anonymizePassenger(profile, actingAdmin);
    }
}
