package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Motivo que el admin escribio al dar de baja esta organizacion. Se borra solo al reactivarla.
    @Column(name = "deactivation_reason", length = 300)
    private String deactivationReason;

    // Marcada por el super admin como "pendiente de eliminacion": queda bloqueada y en revision
    // antes de borrarla definitivamente (ver OrganizationService.deletePermanently).
    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;

    // Marca la UNICA organizacion especial que agrupa a los operadores que se registran solos sin
    // pertenecer a ninguna organizacion contratante (taxistas independientes); se crea sola la
    // primera vez que alguien se registra como independiente.
    @Column(name = "is_independent_pool", nullable = false)
    @Builder.Default
    private boolean independentPool = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
