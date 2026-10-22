package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.driver.DriverPaymentInfoResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.PlatformSettings;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cobro de plataforma por viaje (operador + pasajero) y bloqueo de operadores que no transfieren
 * lo que deben a tiempo. No hay pasarela de pago integrada: el operador transfiere manualmente a
 * la CLABE configurada (ver PlatformSettings) y un admin confirma el pago desde su panel
 * (markPaid), que es lo que de verdad limpia la deuda y reactiva al operador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverBillingService {

    private final DriverProfileRepository driverProfileRepository;
    private final PlatformSettingsService platformSettingsService;

    /**
     * Se llama al completar un viaje (ver TripService.completeTrip): suma al saldo del operador lo
     * que debe por este viaje (su propio cobro + el que le cobro al pasajero) y, si antes no debia
     * nada, fija el plazo para pagar (hora limite configurada del dia siguiente).
     */
    @Transactional
    public void recordCompletedTrip(DriverProfile driver, Trip trip) {
        BigDecimal tripDebt = trip.getOperatorServiceFee().add(trip.getPassengerServiceFee());
        if (tripDebt.signum() <= 0) return;

        boolean hadNoDebtBefore = driver.getPendingAmount().signum() <= 0;
        driver.setPendingAmount(driver.getPendingAmount().add(tripDebt));
        driver.setUnpaidTripCount(driver.getUnpaidTripCount() + 1);
        if (hadNoDebtBefore) {
            int deadlineHour = platformSettingsService.get().getPaymentDeadlineHour();
            driver.setPaymentDueAt(LocalDate.now().plusDays(1).atTime(deadlineHour, 0));
        }
        driverProfileRepository.save(driver);
    }

    @Transactional
    public DriverProfile markPaid(Long driverId) {
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operador no encontrado"));
        driver.setPendingAmount(BigDecimal.ZERO);
        driver.setUnpaidTripCount(0);
        driver.setPaymentDueAt(null);
        driver.setPaymentBlocked(false);
        return driverProfileRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public DriverPaymentInfoResponse getPaymentInfo(User driverUser) {
        DriverProfile driver = driverProfileRepository.findByUser_Id(driverUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        PlatformSettings settings = platformSettingsService.get();
        return new DriverPaymentInfoResponse(
                driver.getPendingAmount(),
                driver.getUnpaidTripCount(),
                settings.getPaymentTripThreshold(),
                driver.getPaymentDueAt(),
                driver.isPaymentBlocked(),
                settings.getClabe(),
                settings.getOperatorFeeType(),
                settings.getOperatorFeeValue(),
                settings.getPassengerFeeType(),
                settings.getPassengerFeeValue()
        );
    }

    public void assertNotBlocked(DriverProfile driver) {
        if (!driver.isPaymentBlocked()) return;
        throw new ApiException(HttpStatus.FORBIDDEN,
                "Tienes un pago pendiente de $" + driver.getPendingAmount() + " MXN. Transfierelo a la CLABE de la plataforma "
                        + "y pide a un administrador que confirme tu pago para poder conectarte de nuevo.");
    }

    /**
     * Revisa periodicamente (igual patron que TripService.retryTripsWithoutDrivers) quien debe
     * dinero y ya sea paso la hora limite del dia siguiente o acumulo demasiados viajes sin pagar
     * (lo que llegue primero) - a esos se les bloquea el acceso a conectarse hasta que un admin
     * confirme su pago.
     */
    @Scheduled(fixedRateString = "${app.billing.overdue-check-interval-ms:300000}")
    @Transactional
    public void blockOverdueDrivers() {
        int tripThreshold = platformSettingsService.get().getPaymentTripThreshold();
        LocalDateTime now = LocalDateTime.now();
        var candidates = driverProfileRepository.findByPendingAmountGreaterThanAndPaymentBlockedFalse(BigDecimal.ZERO);
        int blocked = 0;
        for (DriverProfile driver : candidates) {
            boolean pastDeadline = driver.getPaymentDueAt() != null && now.isAfter(driver.getPaymentDueAt());
            boolean overTripThreshold = driver.getUnpaidTripCount() >= tripThreshold;
            if (pastDeadline || overTripThreshold) {
                driver.setPaymentBlocked(true);
                driverProfileRepository.save(driver);
                blocked++;
            }
        }
        if (blocked > 0) {
            log.info("{} operador(es) bloqueado(s) por pago pendiente vencido", blocked);
        }
    }
}
