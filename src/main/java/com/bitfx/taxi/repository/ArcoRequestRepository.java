package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.ArcoRequest;
import com.bitfx.taxi.model.ArcoRequestStatus;
import com.bitfx.taxi.model.ArcoRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ArcoRequestRepository extends JpaRepository<ArcoRequest, Long> {
    List<ArcoRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<ArcoRequest> findAllByOrderByCreatedAtDesc();
    List<ArcoRequest> findByUser_IdAndTypeAndStatusIn(Long userId, ArcoRequestType type, Collection<ArcoRequestStatus> statuses);
    boolean existsByFolio(String folio);
}
