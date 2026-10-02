package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.CancelledBy;
import com.bitfx.taxi.model.DriverProfile;
import com.bitfx.taxi.model.Trip;
import com.bitfx.taxi.model.TripStatus;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerOrderByRequestedAtDesc(User passenger);
    List<Trip> findByDriverOrderByRequestedAtDesc(DriverProfile driver);
    Optional<Trip> findFirstByPassengerAndStatusIn(User passenger, List<TripStatus> statuses);
    Optional<Trip> findByShareToken(String shareToken);
    Optional<Trip> findFirstByDriverAndStatusIn(DriverProfile driver, List<TripStatus> statuses);
    List<Trip> findByStatusAndScheduledAtBefore(TripStatus status, LocalDateTime before);
    List<Trip> findByStatus(TripStatus status);

    long countByPassenger(User passenger);

    long countByDriverAndStatusAndCompletedAtBetween(DriverProfile driver, TripStatus status, LocalDateTime from, LocalDateTime to);

    long countByDriverAndAcceptedAtBetween(DriverProfile driver, LocalDateTime from, LocalDateTime to);

    long countByDriverAndStatusAndCancelledByRoleAndCancelledAtBetween(
            DriverProfile driver, TripStatus status, CancelledBy cancelledByRole, LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(t.estimatedFare), 0) from Trip t "
            + "where t.driver = :driver and t.status = :status and t.completedAt between :from and :to")
    BigDecimal sumEarningsForDriverBetween(@Param("driver") DriverProfile driver, @Param("status") TripStatus status,
                                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(t.tipAmount), 0) from Trip t "
            + "where t.driver = :driver and t.status = :status and t.completedAt between :from and :to")
    BigDecimal sumTipsForDriverBetween(@Param("driver") DriverProfile driver, @Param("status") TripStatus status,
                                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
