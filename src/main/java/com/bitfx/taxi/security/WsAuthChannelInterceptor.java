package com.bitfx.taxi.security;

import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.TripRepository;
import com.bitfx.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sin esto, el broker STOMP acepta cualquier CONNECT sin credenciales y cualquier SUBSCRIBE a
 * cualquier topic (los ids de viaje/operador en las rutas son secuenciales y adivinables) - un
 * tercero podria escuchar la ubicacion en vivo, el chat o las ofertas de cualquier otro usuario con
 * solo abrir el socket y suscribirse al id que quisiera. Aqui se exige el mismo JWT que ya usa el
 * resto de la API en el frame CONNECT, y en cada SUBSCRIBE se valida que el usuario autenticado sea
 * el pasajero o el operador dueno de ese viaje (o el operador dueno de ese canal de ofertas).
 */
@Component
@RequiredArgsConstructor
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TRIP_TOPIC = Pattern.compile("^/topic/trip/(\\d+)(?:/(?:driver-location|messages))?$");
    private static final Pattern DRIVER_OFFERS_TOPIC = Pattern.compile("^/topic/driver/(\\d+)/offers$");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Falta el token de autenticacion");
        }
        String token = header.substring(7);
        if (!jwtTokenProvider.isValid(token)) {
            throw new MessagingException("Token invalido o expirado");
        }
        User user = userRepository.findByPhone(jwtTokenProvider.getSubject(token))
                .orElseThrow(() -> new MessagingException("Usuario no encontrado"));
        // Mismo chequeo que JwtAuthFilter: si inicio sesion despues en otro dispositivo, este
        // token ya no es la sesion vigente aunque no haya expirado (ver User.sessionVersion).
        if (user.getSessionVersion() != jwtTokenProvider.getSessionVersion(token)) {
            throw new MessagingException("Sesion cerrada desde otro dispositivo");
        }
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;
        User user = currentUser(accessor);

        Matcher tripMatcher = TRIP_TOPIC.matcher(destination);
        if (tripMatcher.matches()) {
            Long tripId = Long.valueOf(tripMatcher.group(1));
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new MessagingException("Viaje no encontrado"));
            boolean isPassenger = trip.getPassenger().getId().equals(user.getId());
            boolean isAssignedDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(user.getId());
            if (!isPassenger && !isAssignedDriver) {
                throw new MessagingException("No autorizado para este viaje");
            }
            return;
        }

        Matcher offersMatcher = DRIVER_OFFERS_TOPIC.matcher(destination);
        if (offersMatcher.matches()) {
            Long driverId = Long.valueOf(offersMatcher.group(1));
            DriverProfile driver = driverProfileRepository.findById(driverId)
                    .orElseThrow(() -> new MessagingException("Operador no encontrado"));
            if (!driver.getUser().getId().equals(user.getId())) {
                throw new MessagingException("No autorizado para este canal de ofertas");
            }
            return;
        }

        // Cualquier otro destino no reconocido se niega por defecto, en vez de intentar enumerar
        // excepciones: es mas seguro que un topic nuevo quede bloqueado hasta agregarlo aqui a que
        // quede abierto por accidente.
        throw new MessagingException("Destino no permitido: " + destination);
    }

    private User currentUser(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new MessagingException("No autenticado");
        }
        return user;
    }
}
