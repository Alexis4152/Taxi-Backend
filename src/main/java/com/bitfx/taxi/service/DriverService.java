package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.AdminCreateDriverRequest;
import com.bitfx.taxi.dto.admin.OnlineDriverSummary;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverLocationRepository;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.OrganizationRepository;
import com.bitfx.taxi.repository.PasswordResetTokenRepository;
import com.bitfx.taxi.repository.RefreshTokenRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import com.bitfx.taxi.repository.TripRepository;
import com.bitfx.taxi.repository.UserRepository;
import com.bitfx.taxi.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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
    public void setActive(Long driverId, boolean active, String reason) {
        DriverProfile driver = getById(driverId);
        driver.setActive(active);
        // El motivo solo tiene sentido mientras esta dado de baja - se borra solo al reactivar,
        // para no dejar un motivo viejo mostrandose si lo vuelven a dar de baja despues sin uno.
        driver.setDeactivationReason(active ? null : reason);
        driverProfileRepository.save(driver);
        User user = driver.getUser();
        user.setActive(active);
        userRepository.save(user);
        if (!active) {
            invalidateSessions(user);
        }
    }

    // Primer paso de "eliminar": bloquea el acceso y lo marca en revision, sin borrar nada todavia
    // (le da tiempo al admin de verificar antes de un borrado definitivo e irreversible).
    @Transactional
    public void markPendingDeletion(Long driverId, String reason) {
        DriverProfile driver = getById(driverId);
        driver.setActive(false);
        driver.setPendingDeletion(true);
        driver.setDeactivationReason(reason);
        driverProfileRepository.save(driver);
        User user = driver.getUser();
        user.setActive(false);
        userRepository.save(user);
        invalidateSessions(user);
    }

    // Bloquear/marcar pendiente de eliminacion no debe dejar utilizable una sesion ya abierta - sin
    // esto, un token valido emitido antes del bloqueo seguia funcionando con normalidad (podia
    // conectarse, recibir viajes, etc.) hasta que expirara solo. Misma idea que
    // AuthService.startNewSession, pero disparada al bloquear en vez de al iniciar sesion.
    private void invalidateSessions(User user) {
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(t -> t.setRevoked(true));
    }

    @Transactional
    public void cancelPendingDeletion(Long driverId) {
        DriverProfile driver = getById(driverId);
        driver.setPendingDeletion(false);
        driverProfileRepository.save(driver);
    }

    // El propio operador solicita que se elimine su cuenta: se bloquea de inmediato su acceso y
    // queda en revision para que el super admin la acepte (y en ese momento se borre definitivo) o
    // la rechace.
    @Transactional
    public void requestOwnDeletion(User user) {
        DriverProfile driver = driverProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No tienes un perfil de operador"));
        driver.setActive(false);
        driver.setPendingDeletion(true);
        driver.setDeactivationReason("El operador solicito la eliminacion de su cuenta");
        driverProfileRepository.save(driver);
        user.setActive(false);
        userRepository.save(user);
        invalidateSessions(user);
    }

    // Borrado definitivo - solo permitido si ya paso por markPendingDeletion. Si tiene viajes,
    // calificaciones u otros registros ligados, la base de datos rechaza el borrado (no se hace
    // cascada manual aqui para no perder historial por error) y se lo reportamos al admin.
    @Transactional
    public void deletePermanently(Long driverId) {
        DriverProfile driver = getById(driverId);
        if (!driver.isPendingDeletion()) {
            throw new ApiException(HttpStatus.CONFLICT, "Primero debes marcarlo como pendiente de eliminacion");
        }
        User user = driver.getUser();
        try {
            driverProfileRepository.delete(driver);
            driverProfileRepository.flush();
            // Puramente tecnicas (sesiones/tokens), no historial de negocio - se borran con la
            // cuenta en vez de bloquear el borrado para siempre (toda cuenta que alguna vez inicio
            // sesion tiene al menos una fila aqui).
            refreshTokenRepository.deleteByUser(user);
            passwordResetTokenRepository.deleteByUser(user);
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el operador tiene viajes u otros registros asociados. Permanece bloqueado como pendiente de eliminacion.");
        }
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return organizationService.resolveIndependentOrganization();
        }
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada"));
    }
}
