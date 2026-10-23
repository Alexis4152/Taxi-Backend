package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // El taxi que este operador maneja - un mismo taxi puede tener varios operadores (lo rotan
    // entre ellos mismos, sin que un admin tenga que asignar turnos); el control de que solo uno
    // este "al volante" a la vez se hace por estatus de conexion (ver AuthService.login), no por
    // una asignacion de horario.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxi_id")
    private Taxi taxi;

    @Column(name = "photo_url", length = 300)
    private String photoUrl;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Motivo que el admin escribio al dar de baja esta cuenta - se le muestra al operador al
    // intentar iniciar sesion (ver AuthService.login). Se borra solo al reactivar.
    @Column(name = "deactivation_reason", length = 300)
    private String deactivationReason;

    // Marcado por el admin como "pendiente de eliminacion": el acceso ya quedo bloqueado (ver
    // active) y se deja en revision antes de borrarlo definitivamente (ver DriverService.deletePermanently).
    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Saldo que este operador debe transferir a la plataforma (cobro de operador + cobro de
    // pasajero de cada viaje completado), cuantos de esos viajes lleva sin pagar, cuando vence el
    // plazo para pagar, y si ya se le bloqueo el acceso a conectarse por no pagar a tiempo - ver
    // DriverBillingService.
    @Column(name = "pending_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Column(name = "unpaid_trip_count", nullable = false)
    @Builder.Default
    private int unpaidTripCount = 0;

    @Column(name = "payment_due_at")
    private LocalDateTime paymentDueAt;

    @Column(name = "payment_blocked", nullable = false)
    @Builder.Default
    private boolean paymentBlocked = false;
}
