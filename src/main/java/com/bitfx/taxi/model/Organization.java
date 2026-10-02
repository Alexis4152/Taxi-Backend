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
