package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.LocationPingRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.DriverLocation;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.TripStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverLocationRepository;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final List<TripStatus> TRACKABLE_STATUSES = List.of(TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    private final DriverLocationRepository driverLocationRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final TripRepository tripRepository;
    private final TripNotifier tripNotifier;
    private final TripService tripService;

    @Transactional
    public void ping(User driverUser, LocationPingRequest req) {
        DriverProfile driver = resolveDriver(driverUser);
        DriverLocation location = driverLocationRepository.findByDriver(driver)
                .orElseGet(() -> DriverLocation.builder().driver(driver).build());
        location.setLat(req.lat());
        location.setLng(req.lng());
        location.setHeading(req.heading());
        location.setOnline(true);
        location.setUpdatedAt(LocalDateTime.now());
        driverLocationRepository.save(location);

        tripRepository.findFirstByDriverAndStatusIn(driver, TRACKABLE_STATUSES)
                .ifPresent(trip -> tripNotifier.notifyDriverLocation(trip.getId(), req.lat(), req.lng(), req.heading()));

        // Se revisa en cada ping (no solo al "conectarse"): el flag de en linea en BD puede quedar
        // trabado en true si el operador cerro la app sin desconectarse antes, lo que haria que la
        // deteccion de "se acaba de conectar" nunca disparara aunque el operador si se vea a si
        // mismo como recien conectado. Es idempotente (no duplica ofertas ya enviadas), asi que
        // revisarlo seguido no tiene costo real.
        tripService.dispatchPendingTripsToNewlyOnlineDriver(driver);
    }

    @Transactional
    public void goOffline(User driverUser) {
        DriverProfile driver = resolveDriver(driverUser);
        driverLocationRepository.findByDriver(driver).ifPresent(loc -> {
            loc.setOnline(false);
            loc.setUpdatedAt(LocalDateTime.now());
            driverLocationRepository.save(loc);
        });
    }

    private DriverProfile resolveDriver(User user) {
        return driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
    }
}
