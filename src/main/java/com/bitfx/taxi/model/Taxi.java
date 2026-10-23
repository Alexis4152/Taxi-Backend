package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "taxis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taxi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;

    @Column(nullable = false, unique = true, length = 15)
    private String plates;

    @Column(length = 60)
    private String brand;

    @Column(length = 60)
    private String model;

    @Column(name = "photo_url", length = 300)
    private String photoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Motivo que el admin escribio al dar de baja este taxi - se le muestra al operador que lo
    // trae vinculado (ver DriverPortalController.myTaxi). Se borra solo al reactivarlo.
    @Column(name = "deactivation_reason", length = 300)
    private String deactivationReason;

    // Marcado por el admin como "pendiente de eliminacion": el taxi ya quedo bloqueado (ver active)
    // y se deja en revision antes de borrarlo definitivamente (ver TaxiService.deletePermanently).
    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;

    @Column(name = "wheelchair_accessible", nullable = false)
    @Builder.Default
    private boolean wheelchairAccessible = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
