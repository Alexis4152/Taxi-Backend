package com.bitfx.taxi.config;

import com.bitfx.taxi.security.JwtAuthFilter;
import com.bitfx.taxi.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/register-driver",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/logout"
                        ).permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Cuando un recurso permitAll (ej. /uploads/**) no existe en disco, Spring
                        // lo reenvia internamente a /error; sin esto Security bloquea ese reenvio
                        // y responde un 401 enganoso en lugar del 404 real.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Patrones (no origenes exactos) para poder incluir rangos de IP de red local (la IP del
        // celular/PC en la red de pruebas cambia); ver app.cors.allowed-origins.
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Aunque ya no se usan cookies para la sesion (el refresh token vive en sessionStorage,
        // no en una cookie httpOnly - ver AuthController), esto DEBE quedar en true: sockjs-client
        // (usado por el STOMP sobre WebSocket) marca "withCredentials = true" de forma fija en
        // cualquier transporte cross-origin (xhr-streaming/xhr-polling, incluida la negociacion
        // inicial /ws/info) - no expone ninguna opcion publica para desactivarlo. En produccion el
        // front (taxi.nexorasistemas.com.mx) y el back (api-taxi.nexorasistemas.com.mx) estan en
        // subdominios distintos, asi que esa peticion siempre es cross-origin: si el backend no
        // responde "Access-Control-Allow-Credentials: true" el navegador bloquea el handshake por
        // completo (no es opcional, lo exige el propio Fetch/CORS spec cuando el request ya trae
        // withCredentials=true), y el operador deja de recibir ofertas de viaje por WebSocket.
        // No reintroduce el riesgo que motivo el cambio a sessionStorage: esto solo permite que el
        // navegador *pueda* mandar cookies si las hubiera, pero el backend no emite ninguna cookie
        // de sesion - la autenticacion real sigue siendo el header Authorization dentro del CONNECT.
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
