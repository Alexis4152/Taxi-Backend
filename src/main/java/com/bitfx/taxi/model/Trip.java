package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private DriverProfile driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxi_id")
    private Taxi taxi;

    @Column(name = "origin_lat", nullable = false)
    private double originLat;

    @Column(name = "origin_lng", nullable = false)
    private double originLng;

    @Column(name = "origin_address", nullable = false, length = 300)
    private String originAddress;

    @Column(name = "destination_lat", nullable = false)
    private double destinationLat;

    @Column(name = "destination_lng", nullable = false)
    private double destinationLng;

    @Column(name = "destination_address", nullable = false, length = 300)
    private String destinationAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TripStatus status;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_min", precision = 8, scale = 2)
    private BigDecimal durationMin;

    @Column(name = "estimated_fare", precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 15)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_confirmed", nullable = false)
    @Builder.Default
    private boolean paymentConfirmed = false;

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by_role", length = 10)
    private CancelledBy cancelledByRole;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    // Geometria de la ruta (lista de [lng, lat]) serializada como JSON, calculada una sola vez al
    // crear el viaje. Antes se recalculaba en cada consulta de estatus llamando al servicio de
    // rutas externo (OSRM); guardarla evita esa dependencia de red para algo que ya no cambia.
    @Column(name = "route_geometry", columnDefinition = "TEXT")
    private String routeGeometryJson;

    // Token opaco para que el pasajero comparta el seguimiento de este viaje con alguien mas sin
    // darle acceso a su cuenta; se genera una sola vez al crear el viaje.
    @Column(name = "share_token", unique = true, length = 40)
    private String shareToken;

    // Propina opcional que el pasajero agrega al calificar al operador, una vez finalizado el viaje.
    @Column(name = "tip_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    // Opciones especiales que el pasajero marca al crear la oferta (silla de bebe, mas de 4
    // pasajeros, mascota) + un comentario libre; se le muestran al operador resaltadas para que
    // decida con esa informacion si toma el viaje.
    @Column(name = "baby_seat", nullable = false)
    @Builder.Default
    private boolean babySeat = false;

    @Column(name = "more_than_four_passengers", nullable = false)
    @Builder.Default
    private boolean moreThanFourPassengers = false;

    @Column(name = "has_pet", nullable = false)
    @Builder.Default
    private boolean hasPet = false;

    @Column(name = "special_comments", length = 300)
    private String specialComments;

    // Cuantas veces se intento despachar este viaje a operadores cercanos (el primer intento al
    // crearlo cuenta como 1); los reintentos amplian el radio de busqueda segun este numero, hasta
    // un maximo configurado, para no perseguir indefinidamente un viaje ya abandonado.
    @Column(name = "dispatch_attempts", nullable = false)
    @Builder.Default
    private int dispatchAttempts = 0;
}
