package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Solicitud de un operador para cambiar de taxi (unidad/placas distintas a la que trae hoy). No se
 * aplica sola: un admin la revisa y aprueba o rechaza; al operador se le avisa por correo en ambos
 * casos.
 */
@Entity
@Table(name = "taxi_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxiChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverProfile driver;

    @Column(name = "requested_unit_number", length = 20)
    private String requestedUnitNumber;

    @Column(name = "requested_plates", nullable = false, length = 15)
    private String requestedPlates;

    @Column(name = "requested_brand", length = 60)
    private String requestedBrand;

    @Column(name = "requested_model", length = 60)
    private String requestedModel;

    @Column(length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaxiChangeRequestStatus status = TaxiChangeRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;
}
