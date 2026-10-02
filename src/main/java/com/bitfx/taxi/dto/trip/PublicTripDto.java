package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.TripStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Version minima del viaje para quien recibe un enlace compartido: sin datos de contacto del
 * pasajero, solo lo necesario para seguir el viaje (estatus, operador/unidad, ubicacion en vivo).
 */
public record PublicTripDto(
        TripStatus status,
        double originLat,
        double originLng,
        String originAddress,
        double destinationLat,
        double destinationLng,
        String destinationAddress,
        List<List<Double>> routeGeometry,
        String driverName,
        String driverPhotoUrl,
        String taxiUnitNumber,
        String taxiPlates,
        Double driverLat,
        Double driverLng,
        LocalDateTime requestedAt
) {
}
