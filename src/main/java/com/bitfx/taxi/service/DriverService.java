package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.AdminCreateDriverRequest;
import com.bitfx.taxi.dto.admin.OnlineDriverSummary;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverLocationRepository;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.OrganizationRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import com.bitfx.taxi.repository.TripRepository;
import com.bitfx.taxi.repository.UserRepository;
import com.bitfx.taxi.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final TripRepository tripRepository;
    private final TaxiRepository taxiRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OrganizationService organizationService;

    public List<DriverProfile> listForOrg(Organization org) {
        return org != null ? driverProfileRepository.findByOrganization(org) : driverProfileRepository.findAll();
    }

    private static final List<TripStatus> ON_TRIP_STATUSES = List.of(TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    public List<OnlineDriverSummary> listOnline(Organization scopedOrg) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return driverLocationRepository.findByOnlineTrue().stream()
                .filter(loc -> scopedOrg == null || loc.getDriver().getOrganization().getId().equals(scopedOrg.getId()))
                .map(loc -> {
                    DriverProfile d = loc.getDriver();
                    Taxi currentTaxi = d.getTaxi();
                    long tripsToday = tripRepository.countByDriverAndStatusAndCompletedAtBetween(d, TripStatus.COMPLETED, startOfDay, endOfDay);
                    boolean onTrip = tripRepository.findFirstByDriverAndStatusIn(d, ON_TRIP_STATUSES).isPresent();
                    return new OnlineDriverSummary(
                            d.getId(), d.getUser().getName(), d.getUser().getPhone(), d.getPhotoUrl(),
                            d.getOrganization() != null ? d.getOrganization().getName() : null,
                            currentTaxi != null ? currentTaxi.getId() : null,
                            currentTaxi != null ? currentTaxi.getUnitNumber() : null,
                            currentTaxi != null ? currentTaxi.getPlates() : null,
                            tripsToday,
                            onTrip,
                            loc.getLat(),
                            loc.getLng(),
                            loc.getHeading()
                    );
                })
                .toList();
    }

    public DriverProfile getById(Long id) {
        return driverProfileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operador no encontrado"));
    }

    @Transactional
    public DriverProfile create(AdminCreateDriverRequest req, Organization scopedOrg) {
        Organization org = scopedOrg != null ? scopedOrg : resolveOrganization(req.organizationId());
        if (userRepository.existsByPhone(req.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese telefono");
        }

        Taxi taxi = null;
        if (req.taxiId() != null) {
            taxi = taxiRepository.findById(req.taxiId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Taxi no encontrado"));
            if (!taxi.getOrganization().getId().equals(org.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El taxi no pertenece a la organizacion del operador");
            }
        }

        String tempPassword = TokenGenerator.generateTemporaryPassword();
        User user = User.builder()
                .phone(req.phone())
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(Role.DRIVER)
                .organization(org)
                .active(true)
                .mustChangePassword(true)
                .build();
        user = userRepository.save(user);

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .organization(org)
                .taxi(taxi)
                .bankAccount(req.bankAccount())
                .address(req.address())
                .active(true)
                .build();
        profile = driverProfileRepository.save(profile);

        emailService.sendDriverWelcomeEmail(req.email(), req.name(), req.phone(), tempPassword);
        return profile;
    }

    /**
     * Cambia el taxi que un operador tiene vinculado (ej. el admin lo corrige, o aprueba una
     * solicitud de cambio). Varios operadores pueden compartir el mismo taxi: no hay validacion de
     * "ya ocupado" aqui, esa se hace al momento de conectarse (ver AuthService.login).
     */
    @Transactional
    public DriverProfile assignTaxi(Long driverId, Long taxiId, Organization scopedOrg) {
        DriverProfile driver = getById(driverId);
        if (scopedOrg != null && !driver.getOrganization().getId().equals(scopedOrg.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El operador no pertenece a tu organizacion");
        }
        if (taxiId == null) {
            driver.setTaxi(null);
            return driverProfileRepository.save(driver);
        }
        Taxi taxi = taxiRepository.findById(taxiId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Taxi no encontrado"));
        if (!taxi.getOrganization().getId().equals(driver.getOrganization().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El taxi no pertenece a la organizacion del operador");
        }
        driver.setTaxi(taxi);
        return driverProfileRepository.save(driver);
    }

    @Transactional
    public DriverProfile updatePhoto(Long driverId, String photoUrl) {
        DriverProfile driver = getById(driverId);
        driver.setPhotoUrl(photoUrl);
        return driverProfileRepository.save(driver);
    }

    @Transactional
    public DriverProfile updateBankAccount(Long driverId, String bankAccount) {
        DriverProfile driver = getById(driverId);
        driver.setBankAccount(bankAccount);
        return driverProfileRepository.save(driver);
    }

    @Transactional
    public DriverProfile update(Long driverId, String name, String email, String bankAccount, String address) {
        DriverProfile driver = getById(driverId);
        driver.setBankAccount(bankAccount);
        driver.setAddress(address);
        driverProfileRepository.save(driver);

        User user = driver.getUser();
        user.setName(name);
        user.setEmail(email);
        userRepository.save(user);
        return driver;
    }

    @Transactional
    public void setActive(Long driverId, boolean active) {
        DriverProfile driver = getById(driverId);
        driver.setActive(active);
        driverProfileRepository.save(driver);
        User user = driver.getUser();
        user.setActive(active);
        userRepository.save(user);
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return organizationService.resolveIndependentOrganization();
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }
}
