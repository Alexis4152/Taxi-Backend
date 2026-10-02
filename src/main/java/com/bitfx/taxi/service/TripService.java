package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.*;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.*;
import com.bitfx.taxi.service.route.RouteEstimate;
import com.bitfx.taxi.util.GeoUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private static final List<TripStatus> ACTIVE_STATUSES =
            List.of(TripStatus.SEARCHING, TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    // Un operador "en viaje" (para el admin) es uno que ya trae pasajero o va en camino a
    // recogerlo - a diferencia de ACTIVE_STATUSES, sin incluir SEARCHING (que ademas nunca tiene
    // operador asignado todavia).
    private static final List<TripStatus> ON_TRIP_STATUSES = List.of(TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    // Para el pasajero, un viaje programado (aun sin despachar) tambien cuenta como "ya tengo un
    // viaje en curso": no puede pedir dos a la vez, uno inmediato y otro programado o dos programados.
    private static final List<TripStatus> PASSENGER_BLOCKING_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.SEARCHING, TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    private final TripRepository tripRepository;
    private final TripOfferRepository tripOfferRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final RoutingService routingService;
    private final FareService fareService;
    private final TripNotifier tripNotifier;
    private final ObjectMapper objectMapper;

    @Value("${app.dispatch.search-radius-km}")
    private double searchRadiusKm;

    @Value("${app.dispatch.max-offers}")
    private int maxOffers;

    @Value("${app.dispatch.offer-ttl-seconds}")
    private int offerTtlSeconds;

    @Value("${app.dispatch.schedule-lead-minutes}")
    private int scheduleLeadMinutes;

    @Value("${app.dispatch.retry-radius-multiplier}")
    private double retryRadiusMultiplier;

    @Value("${app.dispatch.retry-max-attempts}")
    private int retryMaxAttempts;

    @Transactional
    public TripResponseDto requestTrip(User passenger, TripRequestDto req) {
        tripRepository.findFirstByPassengerAndStatusIn(passenger, PASSENGER_BLOCKING_STATUSES).ifPresent(t -> {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tienes un viaje en curso o programado");
        });

        // Una hora futura a mas de 2 minutos de distancia se trata como viaje programado (se
        // despacha solo, mas cerca de la hora pedida); si es null o muy cercana, es inmediato.
        boolean isScheduled = req.scheduledAt() != null && req.scheduledAt().isAfter(LocalDateTime.now().plusMinutes(2));

        RouteEstimate estimate = routingService.estimate(req.originLat(), req.originLng(), req.destinationLat(), req.destinationLng());
        var estimatedFare = fareService.estimateFare(null, estimate.distanceKm());

        Trip trip = Trip.builder()
                .passenger(passenger)
                .originLat(req.originLat())
                .originLng(req.originLng())
                .originAddress(req.originAddress())
                .destinationLat(req.destinationLat())
                .destinationLng(req.destinationLng())
                .destinationAddress(req.destinationAddress())
                .status(isScheduled ? TripStatus.SCHEDULED : TripStatus.SEARCHING)
                .scheduledAt(isScheduled ? req.scheduledAt() : null)
                .distanceKm(estimate.distanceKm())
                .durationMin(estimate.durationMin())
                .estimatedFare(estimatedFare)
                .paymentMethod(req.paymentMethod())
                .routeGeometryJson(toJson(estimate.routeGeometry()))
                .shareToken(java.util.UUID.randomUUID().toString().replace("-", ""))
                .babySeat(req.babySeat())
                .moreThanFourPassengers(req.moreThanFourPassengers())
                .hasPet(req.hasPet())
                .specialComments(req.comments())
                .build();
        trip = tripRepository.save(trip);

        if (!isScheduled) {
            dispatchToNearestDrivers(trip);
        }
        return toDto(trip, estimate.routeGeometry());
    }

    /**
     * Revisa cada cierto tiempo los viajes programados cuya hora ya esta cerca (dentro de
     * app.dispatch.schedule-lead-minutes) y los pasa a busqueda de operador, igual que un viaje
     * inmediato normal.
     */
    @Scheduled(fixedRateString = "${app.dispatch.scheduled-check-interval-ms:30000}")
    @Transactional
    public void dispatchDueScheduledTrips() {
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(scheduleLeadMinutes);
        List<Trip> due = tripRepository.findByStatusAndScheduledAtBefore(TripStatus.SCHEDULED, threshold);
        for (Trip trip : due) {
            trip.setStatus(TripStatus.SEARCHING);
            trip = tripRepository.save(trip);
            log.info("Despachando viaje programado id={} (programado para {})", trip.getId(), trip.getScheduledAt());
            dispatchToNearestDrivers(trip);
            tripNotifier.notifyTripUpdate(trip.getId(), toDto(trip));
        }
    }

    private void dispatchToNearestDrivers(Trip trip) {
        dispatchToNearestDrivers(trip, searchRadiusKm);
    }

    private void dispatchToNearestDrivers(Trip trip, double radiusKm) {
        boolean wasStuck = trip.getStatus() == TripStatus.NO_DRIVERS_AVAILABLE;
        trip.setDispatchAttempts(trip.getDispatchAttempts() + 1);
        final Trip tripRef = trip;

        List<Object[]> candidates = driverLocationRepository.findByOnlineTrue().stream()
                .filter(loc -> loc.getDriver().isActive())
                .filter(loc -> loc.getDriver().getTaxi() != null)
                .filter(loc -> tripRepository.findFirstByDriverAndStatusIn(loc.getDriver(), ACTIVE_STATUSES).isEmpty())
                .filter(loc -> tripOfferRepository.findByTripAndDriver(tripRef, loc.getDriver()).isEmpty())
                .map(loc -> new Object[]{loc, GeoUtils.haversineKm(tripRef.getOriginLat(), tripRef.getOriginLng(), loc.getLat(), loc.getLng())})
                .filter(pair -> (double) pair[1] <= radiusKm)
                .sorted(Comparator.comparingDouble(pair -> (double) pair[1]))
                .limit(maxOffers)
                .toList();

        if (candidates.isEmpty()) {
            if (trip.getStatus() == TripStatus.SEARCHING) {
                trip.setStatus(TripStatus.NO_DRIVERS_AVAILABLE);
            }
            tripRepository.save(trip);
            return;
        }

        // Si estaba sin operadores disponibles y el reintento con radio ampliado si encontro
        // candidatos, se regresa a buscar activamente (y se avisa al pasajero por WS).
        if (wasStuck) {
            trip.setStatus(TripStatus.SEARCHING);
        }
        trip = tripRepository.save(trip);
        if (wasStuck) {
            tripNotifier.notifyTripUpdate(trip.getId(), toDto(trip));
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(offerTtlSeconds);
        for (Object[] pair : candidates) {
            DriverLocation loc = (DriverLocation) pair[0];
            double distanceKm = (double) pair[1];
            sendOfferToDriver(trip, loc, distanceKm, expiresAt);
        }
    }

    private void sendOfferToDriver(Trip trip, DriverLocation loc, double distanceKm, LocalDateTime expiresAt) {
        TripOffer offer = TripOffer.builder()
                .trip(trip)
                .driver(loc.getDriver())
                .status(OfferStatus.SENT)
                .expiresAt(expiresAt)
                .build();
        offer = tripOfferRepository.save(offer);

        TripOfferDto dto = new TripOfferDto(
                offer.getId(), trip.getId(),
                trip.getOriginLat(), trip.getOriginLng(), trip.getOriginAddress(),
                trip.getDestinationLat(), trip.getDestinationLng(), trip.getDestinationAddress(),
                trip.getDistanceKm(), trip.getEstimatedFare(), trip.getPaymentMethod().name(),
                Math.round(distanceKm * 100) / 100.0, offerTtlSeconds,
                trip.getScheduledAt() != null,
                trip.isBabySeat(), trip.isMoreThanFourPassengers(), trip.isHasPet(), trip.getSpecialComments()
        );
        tripNotifier.sendOfferToDriver(loc.getDriver().getId(), dto);
    }

    /**
     * Cuando un operador se conecta (pasa de desconectado a en linea), puede que ya haya viajes
     * esperando operador desde antes - sin esto, se quedaria sin verlos hasta el proximo viaje
     * nuevo que se cree despues de conectarse. Le manda una oferta de una vez por cada uno que le
     * quede dentro de su radio y que todavia no le hayan ofrecido.
     */
    @Transactional
    public void dispatchPendingTripsToNewlyOnlineDriver(DriverProfile driver) {
        if (!driver.isActive()) return;
        if (driver.getTaxi() == null) return;
        if (tripRepository.findFirstByDriverAndStatusIn(driver, ACTIVE_STATUSES).isPresent()) return;

        DriverLocation loc = driverLocationRepository.findByDriver(driver).orElse(null);
        if (loc == null) return;

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(offerTtlSeconds);
        for (Trip trip : tripRepository.findByStatus(TripStatus.SEARCHING)) {
            if (tripOfferRepository.findByTripAndDriver(trip, driver).isPresent()) continue;
            double distanceKm = GeoUtils.haversineKm(trip.getOriginLat(), trip.getOriginLng(), loc.getLat(), loc.getLng());
            if (distanceKm > searchRadiusKm) continue;
            sendOfferToDriver(trip, loc, distanceKm, expiresAt);
        }
    }

    /**
     * Reintenta, cada cierto tiempo, los viajes que se quedaron sin operadores cercanos
     * (NO_DRIVERS_AVAILABLE), ampliando el radio de busqueda en cada intento - hasta un maximo de
     * intentos, para no perseguir indefinidamente un viaje que el pasajero ya abandono.
     */
    @Scheduled(fixedRateString = "${app.dispatch.retry-interval-ms}")
    @Transactional
    public void retryTripsWithoutDrivers() {
        for (Trip trip : tripRepository.findByStatus(TripStatus.NO_DRIVERS_AVAILABLE)) {
            if (trip.getDispatchAttempts() >= retryMaxAttempts) continue;
            double expandedRadius = searchRadiusKm * Math.pow(retryRadiusMultiplier, trip.getDispatchAttempts());
            dispatchToNearestDrivers(trip, expandedRadius);
        }
    }

    @Transactional
    public TripResponseDto acceptOffer(User driverUser, Long tripId, Long offerId) {
        DriverProfile driver = resolveDriver(driverUser);
        Trip trip = getTrip(tripId);
        TripOffer offer = tripOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));

        if (!offer.getDriver().getId().equals(driver.getId()) || !offer.getTrip().getId().equals(trip.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta oferta no te pertenece");
        }
        if (offer.getStatus() != OfferStatus.SENT || offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "La oferta ya no esta disponible");
        }
        if (trip.getStatus() != TripStatus.SEARCHING) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje ya fue tomado por otro operador");
        }
        // Como ahora un operador puede tener varias ofertas pendientes a la vez (ver
        // dispatchToNearestDrivers), hay que evitar que aceptar dos termine asignandole dos viajes
        // activos al mismo tiempo.
        tripRepository.findFirstByDriverAndStatusIn(driver, ACTIVE_STATUSES).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tienes un viaje asignado, no puedes tomar otro a la vez");
        });

        Taxi taxi = driver.getTaxi();
        if (taxi == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes un taxi asignado");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(LocalDateTime.now());
        tripOfferRepository.save(offer);

        trip.setDriver(driver);
        trip.setTaxi(taxi);
        trip.setOrganization(driver.getOrganization());
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());
        trip.setEstimatedFare(fareService.estimateFare(driver.getOrganization(), trip.getDistanceKm()));
        trip = tripRepository.save(trip);

        closeOtherOffers(trip, offer.getId());
        closeOtherPendingOffersForDriver(driver, offer.getId());
        TripResponseDto dto = toDto(trip);
        tripNotifier.notifyTripUpdate(trip.getId(), dto);
        return dto;
    }

    @Transactional
    public void rejectOffer(User driverUser, Long tripId, Long offerId) {
        DriverProfile driver = resolveDriver(driverUser);
        Trip trip = getTrip(tripId);
        TripOffer offer = tripOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
        if (!offer.getDriver().getId().equals(driver.getId()) || !offer.getTrip().getId().equals(trip.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta oferta no te pertenece");
        }
        if (offer.getStatus() == OfferStatus.SENT) {
            offer.setStatus(OfferStatus.REJECTED);
            offer.setRespondedAt(LocalDateTime.now());
            tripOfferRepository.save(offer);
        }

        boolean anyPending = tripOfferRepository.findByTrip(trip).stream()
                .anyMatch(o -> o.getStatus() == OfferStatus.SENT && o.getExpiresAt().isAfter(LocalDateTime.now()));
        if (!anyPending && trip.getStatus() == TripStatus.SEARCHING) {
            trip.setStatus(TripStatus.NO_DRIVERS_AVAILABLE);
            tripRepository.save(trip);
            tripNotifier.notifyTripUpdate(trip.getId(), toDto(trip));
        }
    }

    @Transactional
    public TripResponseDto startTrip(User driverUser, Long tripId) {
        DriverProfile driver = resolveDriver(driverUser);
        Trip trip = getTrip(tripId);
        requireOwnerDriver(trip, driver);
        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje no esta listo para iniciar");
        }
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(LocalDateTime.now());
        trip = tripRepository.save(trip);
        TripResponseDto dto = toDto(trip);
        tripNotifier.notifyTripUpdate(trip.getId(), dto);
        return dto;
    }

    @Transactional
    public TripResponseDto completeTrip(User driverUser, Long tripId) {
        DriverProfile driver = resolveDriver(driverUser);
        Trip trip = getTrip(tripId);
        requireOwnerDriver(trip, driver);
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje no esta en curso");
        }
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now());
        if (trip.getPaymentMethod() == PaymentMethod.CASH) {
            trip.setPaymentConfirmed(true);
        }
        trip = tripRepository.save(trip);
        TripResponseDto dto = toDto(trip);
        tripNotifier.notifyTripUpdate(trip.getId(), dto);
        return dto;
    }

    @Transactional
    public void confirmPayment(User driverUser, Long tripId) {
        DriverProfile driver = resolveDriver(driverUser);
        Trip trip = getTrip(tripId);
        requireOwnerDriver(trip, driver);
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje no ha finalizado");
        }
        trip.setPaymentConfirmed(true);
        tripRepository.save(trip);
    }

    @Transactional
    public TripResponseDto cancelTrip(User user, Long tripId, CancelTripRequest req) {
        Trip trip = getTrip(tripId);
        boolean isPassenger = trip.getPassenger().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(user.getId());
        if (!isPassenger && !isDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes cancelar este viaje");
        }
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "El viaje ya no se puede cancelar");
        }
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());
        trip.setCancelReason(req != null ? req.reason() : null);
        trip.setCancelledByRole(isPassenger ? CancelledBy.PASSENGER : CancelledBy.DRIVER);
        trip = tripRepository.save(trip);

        tripOfferRepository.findByTrip(trip).forEach(o -> {
            if (o.getStatus() == OfferStatus.SENT) {
                o.setStatus(OfferStatus.EXPIRED);
                tripOfferRepository.save(o);
            }
        });

        TripResponseDto dto = toDto(trip);
        tripNotifier.notifyTripUpdate(trip.getId(), dto);
        return dto;
    }

    public TripResponseDto getStatus(User user, Long tripId) {
        Trip trip = getTrip(tripId);
        boolean isPassenger = trip.getPassenger().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(user.getId());
        boolean isStaff = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isPassenger && !isDriver && !isStaff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes ver este viaje");
        }
        return toDto(trip);
    }

    /**
     * Taxis disponibles cerca de un punto, para dibujarlos en el mapa del pasajero mientras arma
     * su solicitud (antes de que exista un viaje). Reutiliza el mismo radio de despacho que
     * dispatchToNearestDrivers, pero es de solo lectura: no crea ofertas ni reserva a nadie.
     */
    public List<NearbyDriverDto> listNearbyAvailableDrivers(double lat, double lng) {
        return driverLocationRepository.findByOnlineTrue().stream()
                .filter(loc -> loc.getDriver().isActive())
                .filter(loc -> loc.getDriver().getTaxi() != null)
                .filter(loc -> tripRepository.findFirstByDriverAndStatusIn(loc.getDriver(), ACTIVE_STATUSES).isEmpty())
                .filter(loc -> GeoUtils.haversineKm(lat, lng, loc.getLat(), loc.getLng()) <= searchRadiusKm)
                .map(loc -> new NearbyDriverDto(loc.getDriver().getId(), loc.getLat(), loc.getLng()))
                .toList();
    }

    public DriverStatsDto getDriverStats(User driverUser) {
        DriverProfile driver = resolveDriver(driverUser);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long completedToday = tripRepository.countByDriverAndStatusAndCompletedAtBetween(driver, TripStatus.COMPLETED, startOfDay, endOfDay);
        long acceptedToday = tripRepository.countByDriverAndAcceptedAtBetween(driver, startOfDay, endOfDay);
        long cancelledByPassengerToday = tripRepository.countByDriverAndStatusAndCancelledByRoleAndCancelledAtBetween(
                driver, TripStatus.CANCELLED, CancelledBy.PASSENGER, startOfDay, endOfDay);
        BigDecimal earningsToday = tripRepository.sumEarningsForDriverBetween(driver, TripStatus.COMPLETED, startOfDay, endOfDay);
        BigDecimal tipsToday = tripRepository.sumTipsForDriverBetween(driver, TripStatus.COMPLETED, startOfDay, endOfDay);

        return new DriverStatsDto(completedToday, acceptedToday, cancelledByPassengerToday, earningsToday, tipsToday);
    }

    public PassengerStatsDto getPassengerStats(User passenger) {
        return new PassengerStatsDto(tripRepository.countByPassenger(passenger));
    }

    public List<TripHistoryItemDto> getMyTripHistoryAsPassenger(User passenger) {
        return tripRepository.findByPassengerOrderByRequestedAtDesc(passenger).stream()
                .map(TripHistoryItemDto::forPassenger)
                .toList();
    }

    public List<TripHistoryItemDto> getMyTripHistoryAsDriver(User driverUser) {
        DriverProfile driver = resolveDriver(driverUser);
        return tripRepository.findByDriverOrderByRequestedAtDesc(driver).stream()
                .map(TripHistoryItemDto::forDriver)
                .toList();
    }

    public TripResponseDto getMyActiveTripAsPassenger(User passenger) {
        return tripRepository.findFirstByPassengerAndStatusIn(passenger, PASSENGER_BLOCKING_STATUSES)
                .map(this::toDto)
                .orElse(null);
    }

    public TripResponseDto getMyActiveTripAsDriver(User driverUser) {
        DriverProfile driver = resolveDriver(driverUser);
        return tripRepository.findFirstByDriverAndStatusIn(driver, ACTIVE_STATUSES)
                .map(this::toDto)
                .orElse(null);
    }

    private void closeOtherOffers(Trip trip, Long acceptedOfferId) {
        tripOfferRepository.findByTrip(trip).forEach(o -> {
            if (!o.getId().equals(acceptedOfferId) && o.getStatus() == OfferStatus.SENT) {
                o.setStatus(OfferStatus.EXPIRED);
                tripOfferRepository.save(o);
                tripNotifier.notifyOfferClosed(o.getDriver().getId(), o.getId());
            }
        });
    }

    /**
     * Un operador puede tener ofertas pendientes de MAS de un viaje a la vez (para que pueda
     * elegir cual tomar). Al aceptar una, las demas que tuviera pendientes se cierran solas: ya no
     * puede atender otro viaje mientras tiene uno asignado. Si algun otro viaje se queda sin
     * ninguna oferta pendiente por esto, se marca sin operadores disponibles.
     */
    private void closeOtherPendingOffersForDriver(DriverProfile driver, Long acceptedOfferId) {
        tripOfferRepository.findByDriverAndStatus(driver, OfferStatus.SENT).forEach(o -> {
            if (o.getId().equals(acceptedOfferId)) return;
            o.setStatus(OfferStatus.EXPIRED);
            tripOfferRepository.save(o);
            tripNotifier.notifyOfferClosed(driver.getId(), o.getId());

            Trip otherTrip = o.getTrip();
            boolean anyPending = tripOfferRepository.findByTrip(otherTrip).stream()
                    .anyMatch(x -> x.getStatus() == OfferStatus.SENT && x.getExpiresAt().isAfter(LocalDateTime.now()));
            if (!anyPending && otherTrip.getStatus() == TripStatus.SEARCHING) {
                otherTrip.setStatus(TripStatus.NO_DRIVERS_AVAILABLE);
                tripRepository.save(otherTrip);
                tripNotifier.notifyTripUpdate(otherTrip.getId(), toDto(otherTrip));
            }
        });
    }

    private TripResponseDto toDto(Trip trip) {
        // La geometria se guarda una sola vez al crear el viaje (ver requestTrip); leerla de ahi
        // evita depender del servicio de rutas externo (o de su cache en memoria) solo para
        // consultar el estatus de un viaje que ya existe. Los viajes creados antes de esta
        // migracion no la tienen guardada: para esos (unicos casos) se recalcula como antes.
        List<List<Double>> routeGeometry = fromJson(trip.getRouteGeometryJson());
        if (routeGeometry == null) {
            RouteEstimate estimate = routingService.estimate(
                    trip.getOriginLat(), trip.getOriginLng(), trip.getDestinationLat(), trip.getDestinationLng());
            routeGeometry = estimate.routeGeometry();
        }
        return toDto(trip, routeGeometry);
    }

    private TripResponseDto toDto(Trip trip, List<List<Double>> routeGeometry) {
        PassengerProfile passengerProfile = passengerProfileRepository.findByUser_Id(trip.getPassenger().getId()).orElse(null);
        return TripResponseDto.from(trip, routeGeometry, passengerProfile);
    }

    private String toJson(List<List<Double>> routeGeometry) {
        try {
            return objectMapper.writeValueAsString(routeGeometry);
        } catch (Exception e) {
            log.warn("No se pudo serializar la geometria de la ruta, se guardara sin ella", e);
            return null;
        }
    }

    private List<List<Double>> fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<List<Double>>>() {
            });
        } catch (Exception e) {
            log.warn("No se pudo leer la geometria de la ruta guardada, se recalculara", e);
            return null;
        }
    }

    /**
     * Viaje que un operador tiene en curso ahora mismo (ya aceptado o ya iniciado), para que un
     * admin (o super admin) pueda ver de donde a donde va, a que hora y quien lo solicito - no
     * solo que esta "en viaje". Valida que el operador pertenezca a la organizacion en turno.
     */
    @Transactional(readOnly = true)
    public TripResponseDto getCurrentTripForDriverAdmin(Organization scopedOrg, Long driverId) {
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operador no encontrado"));
        if (scopedOrg != null && (driver.getOrganization() == null || !driver.getOrganization().getId().equals(scopedOrg.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El operador no pertenece a tu organizacion");
        }
        Trip trip = tripRepository.findFirstByDriverAndStatusIn(driver, ON_TRIP_STATUSES)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El operador no tiene un viaje en curso"));
        return toDto(trip);
    }

    @Transactional(readOnly = true)
    public PublicTripDto getPublicTrip(String shareToken) {
        Trip trip = tripRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enlace de seguimiento invalido"));

        Double driverLat = null;
        Double driverLng = null;
        if (trip.getDriver() != null && ACTIVE_STATUSES.contains(trip.getStatus())) {
            driverLat = driverLocationRepository.findByDriver(trip.getDriver()).map(DriverLocation::getLat).orElse(null);
            driverLng = driverLocationRepository.findByDriver(trip.getDriver()).map(DriverLocation::getLng).orElse(null);
        }

        return new PublicTripDto(
                trip.getStatus(),
                trip.getOriginLat(),
                trip.getOriginLng(),
                trip.getOriginAddress(),
                trip.getDestinationLat(),
                trip.getDestinationLng(),
                trip.getDestinationAddress(),
                fromJson(trip.getRouteGeometryJson()),
                trip.getDriver() != null ? trip.getDriver().getUser().getName() : null,
                trip.getDriver() != null ? trip.getDriver().getPhotoUrl() : null,
                trip.getTaxi() != null ? trip.getTaxi().getUnitNumber() : null,
                trip.getTaxi() != null ? trip.getTaxi().getPlates() : null,
                driverLat,
                driverLng,
                trip.getRequestedAt()
        );
    }

    private Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    }

    private DriverProfile resolveDriver(User user) {
        return driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
    }

    private void requireOwnerDriver(Trip trip, DriverProfile driver) {
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No eres el operador asignado a este viaje");
        }
    }
}
