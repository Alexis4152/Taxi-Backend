package com.bitfx.taxi.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    // Aceptacion del aviso de privacidad (LFPDPPP), independiente de los terminos y condiciones.
    // NULL en cuentas creadas antes de que existiera: se les pide aceptarlo al entrar.
    @Column(name = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;

    // Consentimiento opcional para finalidades secundarias (promociones, novedades). Se puede
    // retirar en cualquier momento sin afectar el servicio.
    @Column(name = "marketing_opt_in", nullable = false)
    @Builder.Default
    private boolean marketingOptIn = false;

    // Fecha en que la cuenta fue anonimizada al aprobarse su eliminacion (derecho de cancelacion) -
    // ver AccountAnonymizationService. Una cuenta anonimizada ya no aparece en los listados.
    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    // Codigo corto propio para invitar a otros (ver ReferralService); se genera una sola vez al
    // registrarse y no cambia despues.
    @Column(name = "referral_code", unique = true, length = 10)
    private String referralCode;

    // Si esta cuenta se registro usando el codigo de otro usuario, quien la invito - null si se
    // registro sin codigo o el codigo no era valido (no se bloquea el registro por eso).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_user_id")
    private User referredBy;

    // Sube en cada login exitoso (ver AuthService.login) para exigir una sola sesion activa a la
    // vez: todo JWT lleva grabada la version vigente al emitirse (claim "sv"), y si no coincide con
    // esta columna, se rechaza aunque no haya expirado todavia - ver JwtAuthFilter y
    // WsAuthChannelInterceptor.
    @Column(name = "session_version", nullable = false)
    @Builder.Default
    private int sessionVersion = 0;

    // Ultima posicion GPS real que reporto esta cuenta (operador en linea, o pasajero con permiso
    // de ubicacion concedido) - sirve solo para centrar el mapa al entrar la proxima vez mientras
    // el GPS en vivo todavia no responde, en vez de abrir siempre en el centro fijo por defecto
    // (ver DEFAULT_MAP_CENTER en el frontend). No se usa para nada mas (no es tracking historico).
    @Column(name = "last_known_lat")
    private Double lastKnownLat;

    @Column(name = "last_known_lng")
    private Double lastKnownLng;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
