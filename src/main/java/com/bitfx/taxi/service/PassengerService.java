package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerProfileRepository passengerProfileRepository;
    private final UserRepository userRepository;

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

    public List<PassengerProfile> listAll() {
        return passengerProfileRepository.findAll();
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
    public void requestOwnDeletion(User user) {
        PassengerProfile profile = passengerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de pasajero"));
        profile.getUser().setActive(false);
        userRepository.save(profile.getUser());
        profile.setPendingDeletion(true);
        profile.setDeactivationReason("El usuario solicito la eliminacion de su cuenta");
        passengerProfileRepository.save(profile);
    }

    // Borrado definitivo - solo permitido si ya paso por markPendingDeletion (admin o el propio
    // usuario). Si tiene viajes, calificaciones u otros registros ligados, la base de datos
    // rechaza el borrado (no se hace cascada manual aqui para no perder historial por error).
    @Transactional
    public void deletePermanently(Long id) {
        PassengerProfile profile = getById(id);
        if (!profile.isPendingDeletion()) {
            throw new ApiException(HttpStatus.CONFLICT, "Primero debe marcarse como pendiente de eliminacion");
        }
        User user = profile.getUser();
        try {
            passengerProfileRepository.delete(profile);
            passengerProfileRepository.flush();
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el usuario tiene viajes u otros registros asociados. Permanece bloqueado como pendiente de eliminacion.");
        }
    }
}
