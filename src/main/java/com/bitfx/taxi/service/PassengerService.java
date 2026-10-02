package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.PassengerProfile;
import com.bitfx.taxi.repository.PassengerProfileRepository;
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
    public void setActive(Long id, boolean active) {
        PassengerProfile profile = getById(id);
        profile.getUser().setActive(active);
        userRepository.save(profile.getUser());
    }
}
