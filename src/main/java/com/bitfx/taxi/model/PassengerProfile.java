package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "passenger_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "photo_url", length = 300)
    private String photoUrl;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    // Motivo de baja (admin) o solicitud propia de eliminacion - se le muestra al usuario al
    // intentar iniciar sesion (ver AuthService.login). Se borra solo al reactivar.
    @Column(name = "deactivation_reason", length = 300)
    private String deactivationReason;

    // Marcado como "pendiente de eliminacion" (por el propio usuario o por el admin): el acceso ya
    // quedo bloqueado y se deja en revision antes de un borrado definitivo (ver
    // PassengerService.deletePermanently).
    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;
}
