package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.TripReportDto;
import com.bitfx.taxi.dto.trip.TripReportResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripReport;
import com.bitfx.taxi.model.TripReportStatus;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.TripReportRepository;
import com.bitfx.taxi.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes de un problema en un viaje, hechos por el pasajero o el operador que lo vivieron. No se
 * resuelven solos: un admin los revisa y los marca como resueltos (igual que TaxiChangeRequest,
 * pero sin un efecto automatico que aplicar - aqui solo importa que alguien lo vea).
 */
@Service
@RequiredArgsConstructor
public class TripReportService {

    private final TripReportRepository tripReportRepository;
    private final TripRepository tripRepository;

    @Transactional
    public TripReportResponse submit(User reporter, Long tripId, TripReportDto req) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        boolean isPassenger = trip.getPassenger().getId().equals(reporter.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(reporter.getId());
        if (!isPassenger && !isDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes reportar un viaje que no es tuyo");
        }

        TripReport report = TripReport.builder()
                .trip(trip)
                .reporter(reporter)
                .category(req.category())
                .description(req.description())
                .status(TripReportStatus.PENDING)
                .build();
        return TripReportResponse.from(tripReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<TripReportResponse> listPending(Organization scopedOrg) {
        return tripReportRepository.findByStatusOrderByCreatedAtAsc(TripReportStatus.PENDING).stream()
                .filter(r -> scopedOrg == null
                        || (r.getTrip().getOrganization() != null && r.getTrip().getOrganization().getId().equals(scopedOrg.getId())))
                .map(TripReportResponse::from)
                .toList();
    }

    @Transactional
    public TripReportResponse resolve(Long reportId, User actingAdmin, Organization scopedOrg) {
        TripReport report = tripReportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        Organization tripOrg = report.getTrip().getOrganization();
        if (scopedOrg != null && (tripOrg == null || !tripOrg.getId().equals(scopedOrg.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este reporte no pertenece a tu organizacion");
        }
        if (report.getStatus() != TripReportStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Este reporte ya fue resuelto");
        }
        report.setStatus(TripReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(actingAdmin);
        return TripReportResponse.from(tripReportRepository.save(report));
    }
}
