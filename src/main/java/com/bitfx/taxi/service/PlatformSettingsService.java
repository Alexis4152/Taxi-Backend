package com.bitfx.taxi.service;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.PlatformSettings;
import com.bitfx.taxi.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Ajustes de la plataforma que el superadmin puede cambiar en caliente (ver
 * PlatformSettingsController) - la fila singleton la crea la migracion V15, nunca hace falta
 * crearla desde aqui.
 */
@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private static final long SINGLETON_ID = 1L;
    private static final int MIN_GEOFENCE_RADIUS_METERS = 10;
    private static final int MAX_GEOFENCE_RADIUS_METERS = 2000;
    private static final double MIN_SEARCH_RADIUS_KM = 0.5;
    private static final double MAX_SEARCH_RADIUS_KM = 50;

    private final PlatformSettingsRepository platformSettingsRepository;

    @Transactional(readOnly = true)
    public PlatformSettings get() {
        return platformSettingsRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "La configuracion de la plataforma no existe"));
    }

    @Transactional
    public PlatformSettings update(int geofenceRadiusMeters, double dispatchSearchRadiusKm, String referralRewardDescription) {
        if (geofenceRadiusMeters < MIN_GEOFENCE_RADIUS_METERS || geofenceRadiusMeters > MAX_GEOFENCE_RADIUS_METERS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El radio debe estar entre " + MIN_GEOFENCE_RADIUS_METERS + " y " + MAX_GEOFENCE_RADIUS_METERS + " metros");
        }
        if (dispatchSearchRadiusKm < MIN_SEARCH_RADIUS_KM || dispatchSearchRadiusKm > MAX_SEARCH_RADIUS_KM) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El radio de busqueda debe estar entre " + MIN_SEARCH_RADIUS_KM + " y " + MAX_SEARCH_RADIUS_KM + " km");
        }
        PlatformSettings settings = get();
        settings.setTripGeofenceRadiusMeters(geofenceRadiusMeters);
        settings.setDispatchSearchRadiusKm(dispatchSearchRadiusKm);
        settings.setReferralRewardDescription(referralRewardDescription);
        settings.setUpdatedAt(LocalDateTime.now());
        return platformSettingsRepository.save(settings);
    }
}
