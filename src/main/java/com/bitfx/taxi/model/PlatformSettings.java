package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila unica (singleton, id siempre 1) con ajustes de la plataforma que el superadmin puede
 * cambiar en caliente, sin redeploy. Hoy solo tiene el radio de geofence para iniciar/finalizar
 * viajes; si en el futuro hacen falta mas ajustes de este tipo, se agregan aqui mismo.
 */
@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettings {

    @Id
    private Long id;

    @Column(name = "trip_geofence_radius_meters", nullable = false)
    private int tripGeofenceRadiusMeters;

    // A cuantos km se le ofrece un viaje nuevo a un operador en linea, y a cuantos km un pasajero
    // ve taxis cercanos en su mapa antes de pedir un viaje (mismo radio para ambos casos) - ver
    // TripService.dispatchToNearestDrivers/listNearbyAvailableDrivers.
    @Column(name = "dispatch_search_radius_km", nullable = false)
    private double dispatchSearchRadiusKm;

    // Texto libre que el superadmin define para explicar que gana quien invita a alguien con su
    // codigo (ver ReferralService) - no hay billetera/credito automatico en la app todavia, asi que
    // el beneficio se aplica manualmente fuera de la app; esto es solo lo que se le muestra al
    // usuario para que sepa que esperar.
    @Column(name = "referral_reward_description", length = 300)
    private String referralRewardDescription;

    // CLABE de la plataforma a la que los operadores transfieren lo que deben (ver
    // DriverBillingService) - no hay pasarela de pago integrada, es solo el numero que se le
    // muestra al operador para que transfiera manualmente.
    @Column(length = 18)
    private String clabe;

    // Cobro por viaje que se le carga al OPERADOR (se acumula en su saldo por pagar, no se le
    // descuenta nada al pasajero por este) - ver DriverBillingService.computeFee.
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_fee_type", nullable = false, length = 10)
    @Builder.Default
    private FeeType operatorFeeType = FeeType.FIXED;

    @Column(name = "operator_fee_value", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal operatorFeeValue = BigDecimal.ZERO;

    // Cobro por viaje que se le SUMA al pasajero sobre la tarifa (lo paga el pasajero, lo cobra el
    // operador en el momento junto con la tarifa, y el operador se lo debe a la plataforma igual
    // que su propio cobro) - ver TripService.requestTrip/acceptOffer.
    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_fee_type", nullable = false, length = 10)
    @Builder.Default
    private FeeType passengerFeeType = FeeType.FIXED;

    @Column(name = "passenger_fee_value", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal passengerFeeValue = BigDecimal.ZERO;

    // Hora (0-23) del dia siguiente a partir de la cual se considera vencido el pago si el
    // operador no ha transferido lo que debe - ver DriverBillingService.
    @Column(name = "payment_deadline_hour", nullable = false)
    @Builder.Default
    private int paymentDeadlineHour = 12;

    // Si el operador acumula esta cantidad de viajes sin pagar, se bloquea de inmediato aunque
    // todavia no llegue la hora limite del dia siguiente.
    @Column(name = "payment_trip_threshold", nullable = false)
    @Builder.Default
    private int paymentTripThreshold = 20;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
