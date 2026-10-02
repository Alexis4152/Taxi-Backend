package com.bitfx.taxi.dto.trip;

import com.bitfx.taxi.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TripRequestDto(
        @NotNull double originLat,
        @NotNull double originLng,
        @NotBlank(message = "La direccion de origen es obligatoria") String originAddress,
        @NotNull double destinationLat,
        @NotNull double destinationLng,
        @NotBlank(message = "La direccion de destino es obligatoria") String destinationAddress,
        @NotNull(message = "El metodo de pago es obligatorio") PaymentMethod paymentMethod,
        // Opcional: hora futura para un viaje programado. Null (o una hora muy cercana/pasada) es
        // un viaje inmediato, igual que antes.
        LocalDateTime scheduledAt,
        // Opciones especiales ("crear oferta"): se le muestran resaltadas al operador para que
        // decida si toma el viaje con esa informacion. Todas opcionales.
        boolean babySeat,
        boolean moreThanFourPassengers,
        boolean hasPet,
        @Size(max = 300, message = "El comentario es muy largo") String comments
) {
}
