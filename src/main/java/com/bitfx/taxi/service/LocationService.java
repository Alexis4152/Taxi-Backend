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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final List<TripStatus> TRACKABLE_STATUSES = List.of(TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    private final DriverLocationRepository driverLocationRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final TripRepository tripRepository;
    private final TripNotifier tripNotifier;
    private final TripService tripService;
    private final DriverBillingService driverBillingService;

    @Value("${app.driver.stale-timeout-minutes}")
    private long staleTimeoutMinutes;

    @Transactional
    public void ping(User driverUser, LocationPingRequest req) {
        DriverProfile driver = resolveDriver(driverUser);
        // Si ya trae un viaje asignado (lo acepto antes de que lo bloquearan), se le deja seguir
        // mandando ubicacion para que ese viaje termine con normalidad (el pasajero lo sigue viendo
        // en el mapa) - el bloqueo es para impedir que tome viajes NUEVOS, no para cortarle uno que
        // ya esta en curso.
        boolean hasActiveTrip = tripRepository.findFirstByDriverAndStatusIn(driver, TRACKABLE_STATUSES).isPresent();
        if (!hasActiveTrip) {
            driverBillingService.assertNotBlocked(driver);
        }
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

    /**
     * Si un operador deja de mandar ping (cerro la app, se quedo sin bateria/señal) sin pasar por
     * goOffline, su fila se quedaria "en linea" para siempre y seguiria recibiendo ofertas de
     * viajes que ya no podria atender. Aqui se revisa periodicamente y se marca fuera de linea a
     * quien no reporte ubicacion en mas de app.driver.stale-timeout-minutes.
     */
    @Scheduled(fixedRateString = "${app.driver.stale-check-interval-ms}")
    @Transactional
    public void markStaleDriversOffline() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleTimeoutMinutes);
        List<DriverLocation> stale = driverLocationRepository.findByOnlineTrueAndUpdatedAtBefore(cutoff);
        if (stale.isEmpty()) return;
        stale.forEach(loc -> loc.setOnline(false));
        driverLocationRepository.saveAll(stale);
        log.info("{} operador(es) marcado(s) fuera de linea por inactividad (sin ping hace mas de {} min)",
                stale.size(), staleTimeoutMinutes);
    }

    private DriverProfile resolveDriver(User user) {
        return driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
    }
}
