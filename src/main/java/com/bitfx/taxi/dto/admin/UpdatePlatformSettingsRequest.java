package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.FeeType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePlatformSettingsRequest(
        @NotNull(message = "El radio es obligatorio")
        @Min(value = 10, message = "El radio debe ser de al menos 10 metros")
        @Max(value = 5000, message = "El radio no puede superar 5000 metros")
        Integer tripGeofenceRadiusMeters,
        @NotNull(message = "El radio de busqueda es obligatorio")
        @DecimalMin(value = "0.5", message = "El radio debe ser de al menos 0.5 km")
        @DecimalMax(value = "50", message = "El radio no puede superar 50 km")
        Double dispatchSearchRadiusKm,
        @Size(max = 300, message = "El texto es muy largo")
        String referralRewardDescription,
        @Pattern(regexp = "^$|^[0-9]{18}$", message = "La CLABE debe tener 18 digitos")
        String clabe,
        @NotNull(message = "El tipo de cobro al operador es obligatorio")
        FeeType operatorFeeType,
        @NotNull(message = "El cobro al operador es obligatorio")
        @DecimalMin(value = "0", message = "El cobro no puede ser negativo")
        BigDecimal operatorFeeValue,
        @NotNull(message = "El tipo de cobro al pasajero es obligatorio")
        FeeType passengerFeeType,
        @NotNull(message = "El cobro al pasajero es obligatorio")
        @DecimalMin(value = "0", message = "El cobro no puede ser negativo")
        BigDecimal passengerFeeValue,
        @NotNull(message = "La hora limite es obligatoria")
        @Min(value = 0, message = "La hora debe estar entre 0 y 23")
        @Max(value = 23, message = "La hora debe estar entre 0 y 23")
        Integer paymentDeadlineHour,
        @NotNull(message = "El limite de viajes es obligatorio")
        @Min(value = 1, message = "Debe ser al menos 1 viaje")
        Integer paymentTripThreshold
) {
}
