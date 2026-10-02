package com.bitfx.taxi.config;

import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.*;
import com.bitfx.taxi.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Siembra los datos minimos para poder probar el sistema apenas se levanta: un SUPER_ADMIN,
 * la tarifa por defecto de la plataforma, y una organizacion + admin + operador + taxi de
 * demostracion (con turno activo) para poder iniciar sesion de inmediato sin configurar nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PassengerProfileRepository passengerProfileRepository;
    private final TaxiRepository taxiRepository;
    private final TariffRuleRepository tariffRuleRepository;
    private final ShiftService shiftService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled}")
    private boolean seedEnabled;

    @Value("${app.seed.super-admin-phone}")
    private String superAdminPhone;

    @Value("${app.seed.super-admin-password}")
    private String superAdminPassword;

    @Value("${app.seed.super-admin-email}")
    private String superAdminEmail;

    private User superAdmin;

    @Override
    @Transactional
    public void run(String... args) {
        // Migracion de datos (no de seed de demo): siempre corre, incluso con el seed deshabilitado,
        // para que operadores que ya tenian un turno vigente bajo el sistema anterior queden
        // vinculados directo a ese taxi bajo el sistema nuevo (sin turnos).
        backfillDriverTaxiFromShifts();
        if (!seedEnabled) {
            return;
        }
        seedSuperAdmin();
        seedDefaultTariff();
        seedDemoOrganization();
    }

    private void backfillDriverTaxiFromShifts() {
        for (DriverProfile driver : driverProfileRepository.findAll()) {
            if (driver.getTaxi() != null) continue;
            shiftService.resolveCurrentForDriver(driver).ifPresent(shift -> {
                driver.setTaxi(shift.getTaxi());
                driverProfileRepository.save(driver);
                log.info("Operador id={} migrado de turno a taxi directo (taxi id={})", driver.getId(), shift.getTaxi().getId());
            });
        }
    }

    private void seedSuperAdmin() {
        superAdmin = userRepository.findByPhone(superAdminPhone).orElse(null);
        if (superAdmin != null) {
            return;
        }
        superAdmin = User.builder()
                .phone(superAdminPhone)
                .name("Super Admin BITFX")
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .role(Role.SUPER_ADMIN)
                .active(true)
                .build();
        superAdmin = userRepository.save(superAdmin);
        log.info("Usuario SUPER_ADMIN semilla creado: telefono={}", superAdminPhone);
    }

    private void seedDefaultTariff() {
        if (tariffRuleRepository.findByOrganizationIsNull().isPresent()) {
            return;
        }
        var tariff = com.bitfx.taxi.model.TariffRule.builder()
                .organization(null)
                .baseFare(new BigDecimal("15.00"))
                .perKm(new BigDecimal("8.00"))
                .minFare(new BigDecimal("30.00"))
                .build();
        tariffRuleRepository.save(tariff);
        log.info("Tarifa por defecto de plataforma creada");
    }

    private void seedDemoOrganization() {
        if (organizationRepository.count() > 0) {
            return;
        }
        Organization org = organizationRepository.save(Organization.builder()
                .name("Taxis Demo BITFX")
                .contactPhone("5550000000")
                .active(true)
                .build());

        Taxi taxi = taxiRepository.save(Taxi.builder()
                .organization(org)
                .unitNumber("001")
                .plates("DEMO-01A")
                .brand("Nissan")
                .model("Versa")
                .active(true)
                .build());

        User driverUser = userRepository.save(User.builder()
                .phone("5550000002")
                .name("Operador Demo")
                .email("operador.demo@bitfx.mx")
                .passwordHash(passwordEncoder.encode("Driver123!"))
                .role(Role.DRIVER)
                .organization(org)
                .active(true)
                .termsAcceptedAt(LocalDateTime.now())
                .build());

        driverProfileRepository.save(DriverProfile.builder()
                .user(driverUser)
                .organization(org)
                .taxi(taxi)
                .bankAccount(null)
                .active(true)
                .build());

        User passengerUser = userRepository.save(User.builder()
                .phone("5550000003")
                .name("Pasajero Demo")
                .email("pasajero.demo@bitfx.mx")
                .passwordHash(passwordEncoder.encode("Pasajero123!"))
                .role(Role.PASSENGER)
                .active(true)
                .termsAcceptedAt(LocalDateTime.now())
                .build());
        passengerProfileRepository.save(PassengerProfile.builder().user(passengerUser).build());

        log.info("Organizacion, operador, pasajero y taxi de demostracion creados (org id={})", org.getId());
    }
}
