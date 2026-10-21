package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
