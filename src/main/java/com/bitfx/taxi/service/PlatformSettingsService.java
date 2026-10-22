package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.UpdatePlatformSettingsRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.FeeType;
import com.bitfx.taxi.model.PlatformSettings;
import com.bitfx.taxi.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Ajustes de la plataforma que el superadmin puede cambiar en caliente (ver
 * PlatformSettingsController) - la fila singleton la crea la migracion V15, nunca hace falta
 * crearla desde aqui.
 */
@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private static final long SINGLETON_ID = 1L;
    private static final int MIN_GEOFENCE_RADIUS_METERS = 10;
    private static final int MAX_GEOFENCE_RADIUS_METERS = 2000;
    private static final double MIN_SEARCH_RADIUS_KM = 0.5;
    private static final double MAX_SEARCH_RADIUS_KM = 50;
    private static final BigDecimal MAX_FIXED_FEE = BigDecimal.valueOf(500);
    private static final BigDecimal MAX_PERCENT_FEE = BigDecimal.valueOf(100);

    private final PlatformSettingsRepository platformSettingsRepository;

    @Transactional(readOnly = true)
    public PlatformSettings get() {
        return platformSettingsRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "La configuracion de la plataforma no existe"));
    }

    @Transactional
    public PlatformSettings update(UpdatePlatformSettingsRequest req) {
        if (req.tripGeofenceRadiusMeters() < MIN_GEOFENCE_RADIUS_METERS || req.tripGeofenceRadiusMeters() > MAX_GEOFENCE_RADIUS_METERS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El radio debe estar entre " + MIN_GEOFENCE_RADIUS_METERS + " y " + MAX_GEOFENCE_RADIUS_METERS + " metros");
        }
        if (req.dispatchSearchRadiusKm() < MIN_SEARCH_RADIUS_KM || req.dispatchSearchRadiusKm() > MAX_SEARCH_RADIUS_KM) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El radio de busqueda debe estar entre " + MIN_SEARCH_RADIUS_KM + " y " + MAX_SEARCH_RADIUS_KM + " km");
        }
        validateFee("operador", req.operatorFeeType(), req.operatorFeeValue());
        validateFee("pasajero", req.passengerFeeType(), req.passengerFeeValue());

        PlatformSettings settings = get();
        settings.setTripGeofenceRadiusMeters(req.tripGeofenceRadiusMeters());
        settings.setDispatchSearchRadiusKm(req.dispatchSearchRadiusKm());
        settings.setReferralRewardDescription(req.referralRewardDescription());
        settings.setClabe(req.clabe() == null || req.clabe().isBlank() ? null : req.clabe());
        settings.setOperatorFeeType(req.operatorFeeType());
        settings.setOperatorFeeValue(req.operatorFeeValue());
        settings.setPassengerFeeType(req.passengerFeeType());
        settings.setPassengerFeeValue(req.passengerFeeValue());
        settings.setPaymentDeadlineHour(req.paymentDeadlineHour());
        settings.setPaymentTripThreshold(req.paymentTripThreshold());
        settings.setUpdatedAt(LocalDateTime.now());
        return platformSettingsRepository.save(settings);
    }

    private void validateFee(String label, FeeType type, BigDecimal value) {
        BigDecimal max = type == FeeType.PERCENT ? MAX_PERCENT_FEE : MAX_FIXED_FEE;
        if (value.compareTo(max) > 0) {
            String unit = type == FeeType.PERCENT ? "%" : " pesos";
            throw new ApiException(HttpStatus.BAD_REQUEST, "El cobro al " + label + " no puede superar " + max + unit);
        }
    }

    /**
     * Cuanto se cobra por un viaje con esta tarifa base, segun si el cobro vigente es un monto fijo
     * o un % de la tarifa - usado tanto para el cobro del operador como el del pasajero (ver
     * TripService.requestTrip/acceptOffer).
     */
    public static BigDecimal computeFee(FeeType type, BigDecimal value, BigDecimal baseFare) {
        if (type == FeeType.PERCENT) {
            return baseFare.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
