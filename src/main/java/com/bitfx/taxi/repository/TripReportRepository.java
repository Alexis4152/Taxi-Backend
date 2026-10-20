package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.TripReport;
import com.bitfx.taxi.model.TripReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripReportRepository extends JpaRepository<TripReport, Long> {
    List<TripReport> findByStatusOrderByCreatedAtAsc(TripReportStatus status);
    List<TripReport> findByReporter_IdOrderByCreatedAtDesc(Long reporterId);
}
