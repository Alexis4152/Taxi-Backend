package com.bitfx.taxi.service;

import com.bitfx.taxi.model.DriverLocation;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.PlatformSettings;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.repository.DriverLocationRepository;
import com.bitfx.taxi.repository.DriverProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverBillingServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private DriverLocationRepository driverLocationRepository;

    @Mock
    private PlatformSettingsService platformSettingsService;

    private DriverBillingService driverBillingService;

    @BeforeEach
    void setUp() {
        driverBillingService = new DriverBillingService(driverProfileRepository, driverLocationRepository, platformSettingsService);
    }

    private static DriverProfile driverWithoutDebt() {
        DriverProfile driver = new DriverProfile();
        driver.setPendingAmount(BigDecimal.ZERO);
        driver.setUnpaidTripCount(0);
        return driver;
    }

    private static Trip tripWithFees() {
        Trip trip = new Trip();
        trip.setOperatorServiceFee(new BigDecimal("5.00"));
        trip.setPassengerServiceFee(new BigDecimal("3.00"));
        return trip;
    }

    @Test
    void alLlegarAlLimiteDeViajesSinPagarSeBloqueaYSaleDeLineaAlMomento() {
        when(platformSettingsService.get()).thenReturn(PlatformSettings.builder().paymentTripThreshold(1).paymentDeadlineHour(12).build());
        DriverProfile driver = driverWithoutDebt();
        DriverLocation location = DriverLocation.builder().driver(driver).online(true).build();
        when(driverLocationRepository.findByDriver(driver)).thenReturn(Optional.of(location));

        driverBillingService.recordCompletedTrip(driver, tripWithFees());

        assertTrue(driver.isPaymentBlocked());
        assertFalse(location.isOnline());
    }

    @Test
    void siNoLlegaAlLimiteSigueConectado() {
        when(platformSettingsService.get()).thenReturn(PlatformSettings.builder().paymentTripThreshold(3).paymentDeadlineHour(12).build());
        DriverProfile driver = driverWithoutDebt();
        lenient().when(driverLocationRepository.findByDriver(driver)).thenReturn(Optional.empty());

        driverBillingService.recordCompletedTrip(driver, tripWithFees());

        assertFalse(driver.isPaymentBlocked());
    }
}
