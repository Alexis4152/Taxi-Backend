package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private DriverProfile driver;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    private Double heading;

    @Column(nullable = false)
    @Builder.Default
    private boolean online = false;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
