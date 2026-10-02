package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.ShiftRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.ShiftRepository;
import com.bitfx.taxi.repository.ShiftTemplateRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turnos con dos modalidades:
 *  - "fijo" (temporary=false): referencia una plantilla de turno (ShiftTemplate, ej. "Turno
 *    Mañana" 06:00-18:00) que el admin dio de alta; vigente cada dia dentro de ese horario, desde
 *    su inicio hasta que se revoca o se reemplaza. Es la asignacion normal de un operador a su
 *    unidad.
 *  - "temporal" (temporary=true): tiene una ventana de fecha/hora libre [startAt, endAt) y se
 *    activa/desactiva solo segun el momento actual, sin que el admin tenga que volver a tocarlo.
 *    Mientras esta dentro de su ventana, manda sobre el turno fijo (para cubrir una unidad que no
 *    es la suya un dia).
 * No hay una columna "activo": la vigencia se calcula en el momento (isCurrentlyInEffect), y
 * "revoked" es solo el apagado manual anticipado (ej. el admin termina el turno antes de tiempo).
 */
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final TaxiRepository taxiRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;

    public List<Shift> listForTaxi(Taxi taxi) {
        return shiftRepository.findByTaxiOrderByStartAtDesc(taxi);
    }

    public List<Shift> listForDriver(DriverProfile driver) {
        return shiftRepository.findByDriverOrderByStartAtDesc(driver);
    }

    public Optional<Shift> resolveCurrentForDriver(DriverProfile driver) {
        LocalDateTime now = LocalDateTime.now();
        return shiftRepository.findByDriverAndRevokedFalse(driver).stream()
                .filter(s -> isCurrentlyInEffect(s, now))
                .max(Comparator.comparing(Shift::isTemporary));
    }

    public Optional<Shift> resolveCurrentForTaxi(Taxi taxi) {
        LocalDateTime now = LocalDateTime.now();
        return shiftRepository.findByTaxiAndRevokedFalse(taxi).stream()
                .filter(s -> isCurrentlyInEffect(s, now))
                .max(Comparator.comparing(Shift::isTemporary));
    }

    public boolean isCurrentlyInEffect(Shift s) {
        return isCurrentlyInEffect(s, LocalDateTime.now());
    }

    boolean isCurrentlyInEffect(Shift s, LocalDateTime now) {
        if (s.isRevoked()) return false;
        if (s.getStartAt() != null && s.getStartAt().isAfter(now)) return false;
        if (s.isTemporary()) {
            return s.getEndAt() != null && s.getEndAt().isAfter(now);
        }
        // Fijo: sin plantilla (turnos antiguos, compatibilidad) sigue vigente las 24h; con
        // plantilla, solo dentro de su horario diario (soporta cruzar medianoche, ej 18:00-06:00).
        if (s.getShiftTemplate() == null) return true;
        return isWithinDailyWindow(s.getShiftTemplate().getStartTime(), s.getShiftTemplate().getEndTime(), now.toLocalTime());
    }

    private boolean isWithinDailyWindow(LocalTime start, LocalTime end, LocalTime now) {
        if (start.equals(end)) return true; // cubre las 24h
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    @Transactional
    public Shift assign(ShiftRequest req, User createdBy, Organization scopedOrg) {
        Taxi taxi = taxiRepository.findById(req.taxiId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Taxi no encontrado"));
        DriverProfile driver = driverProfileRepository.findById(req.driverId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operador no encontrado"));

        if (scopedOrg != null && (!taxi.getOrganization().getId().equals(scopedOrg.getId())
                || !driver.getOrganization().getId().equals(scopedOrg.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El taxi o el operador no pertenecen a tu organizacion");
        }
        if (!taxi.getOrganization().getId().equals(driver.getOrganization().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El taxi y el operador deben ser de la misma organizacion");
        }
        if (!taxi.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El taxi esta dado de baja");
        }
        if (!driver.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El operador esta dado de baja");
        }

        LocalDateTime startAt = req.startAt() != null ? req.startAt() : LocalDateTime.now();
        ShiftTemplate template = null;

        if (req.temporary()) {
            if (req.endAt() == null || !req.endAt().isAfter(startAt)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Un turno temporal necesita una fecha/hora de fin posterior al inicio");
            }
            // Un turno temporal nuevo reemplaza cualquier otro turno temporal que se cruce en el
            // tiempo para ese mismo taxi o ese mismo operador (el turno fijo de ambos no se toca:
            // sigue vigente automaticamente en cuanto termine la ventana temporal).
            revokeOverlapping(shiftRepository.findByTaxiAndRevokedFalse(taxi), startAt, req.endAt(), true);
            revokeOverlapping(shiftRepository.findByDriverAndRevokedFalse(driver), startAt, req.endAt(), true);
        } else {
            if (req.endAt() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Un turno fijo no lleva fecha de fin, elige una plantilla de horario");
            }
            if (req.shiftTemplateId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Un turno fijo necesita una plantilla de turno (ej. Mañana, Noche)");
            }
            template = shiftTemplateRepository.findById(req.shiftTemplateId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plantilla de turno no encontrada"));
            if (!template.getOrganization().getId().equals(taxi.getOrganization().getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La plantilla de turno no pertenece a esta organizacion");
            }
            if (!template.isActive()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Esa plantilla de turno esta desactivada");
            }
            // Un turno fijo nuevo reemplaza al turno fijo anterior de ese taxi y al turno fijo
            // anterior de ese operador (solo puede haber un operador "dueno" de la unidad a la vez).
            revokeFixed(shiftRepository.findByTaxiAndRevokedFalse(taxi));
            revokeFixed(shiftRepository.findByDriverAndRevokedFalse(driver));
        }

        Shift shift = Shift.builder()
                .taxi(taxi)
                .driver(driver)
                .startAt(startAt)
                .endAt(req.endAt())
                .temporary(req.temporary())
                .shiftTemplate(template)
                .revoked(false)
                .createdBy(createdBy)
                .build();
        return shiftRepository.save(shift);
    }

    @Transactional
    public void end(Long shiftId, Organization scopedOrg) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        if (scopedOrg != null && !shift.getTaxi().getOrganization().getId().equals(scopedOrg.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El turno no pertenece a tu organizacion");
        }
        shift.setRevoked(true);
        shiftRepository.save(shift);
    }

    private void revokeFixed(List<Shift> shifts) {
        shifts.stream()
                .filter(s -> !s.isTemporary())
                .forEach(s -> {
                    s.setRevoked(true);
                    shiftRepository.save(s);
                });
    }

    private void revokeOverlapping(List<Shift> shifts, LocalDateTime startAt, LocalDateTime endAt, boolean onlyTemporary) {
        shifts.stream()
                .filter(s -> !onlyTemporary || s.isTemporary())
                .filter(s -> overlaps(s, startAt, endAt))
                .forEach(s -> {
                    s.setRevoked(true);
                    shiftRepository.save(s);
                });
    }

    private boolean overlaps(Shift s, LocalDateTime startAt, LocalDateTime endAt) {
        LocalDateTime existingStart = s.getStartAt();
        LocalDateTime existingEnd = s.getEndAt() != null ? s.getEndAt() : LocalDateTime.MAX;
        return existingStart.isBefore(endAt) && startAt.isBefore(existingEnd);
    }
}
