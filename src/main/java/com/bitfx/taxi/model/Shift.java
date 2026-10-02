package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxi_id", nullable = false)
    private Taxi taxi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverProfile driver;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean temporary = false;

    // Solo aplica a turnos fijos (temporary=false): define el horario diario (ej. 06:00-18:00).
    // Los turnos temporales no llevan plantilla, usan start_at/end_at libres.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_template_id")
    private ShiftTemplate shiftTemplate;

    // false = vigente (si esta dentro de su ventana de tiempo); true = terminado manualmente
    // antes de tiempo por un admin. La vigencia real se calcula en ShiftService segun start_at/
    // end_at, no con un flag que haya que mantener a mano.
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
