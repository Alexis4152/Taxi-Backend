package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.TripReport;
import com.bitfx.taxi.model.TripReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripReportRepository extends JpaRepository<TripReport, Long> {
    List<TripReport> findByStatusOrderByCreatedAtAsc(TripReportStatus status);
    List<TripReport> findByReporter_IdOrderByCreatedAtDesc(Long reporterId);

    // Resueltos se listan del mas reciente al mas viejo (lo contrario de pendientes, donde importa
    // atender primero al que lleva mas tiempo esperando) - ver TripReportService.list.
    List<TripReport> findByStatusOrderByResolvedAtDesc(TripReportStatus status);
    List<TripReport> findAllByOrderByCreatedAtDesc();

    // Para el enlace publico de seguimiento (ver TripService.getPublicTrip): solo importa si
    // existe algun reporte de este viaje, no el contenido (eso no se expone sin autenticarse).
    boolean existsByTrip_Id(Long tripId);
}
