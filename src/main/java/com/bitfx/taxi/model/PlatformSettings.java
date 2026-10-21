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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
