package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.RatingRequest;
import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.RatingRepository;
import com.bitfx.taxi.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PassengerProfileRepository passengerProfileRepository;

    @Transactional
    public Rating submit(User fromUser, RatingRequest req) {
        Trip trip = tripRepository.findById(req.tripId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo puedes calificar viajes completados");
        }

        boolean isPassenger = trip.getPassenger().getId().equals(fromUser.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(fromUser.getId());
        if (!isPassenger && !isDriver) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No participaste en este viaje");
        }

        RatingDirection direction = isPassenger ? RatingDirection.PASSENGER_TO_DRIVER : RatingDirection.DRIVER_TO_PASSENGER;
        if (ratingRepository.existsByTripAndDirection(trip, direction)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya calificaste este viaje");
        }

        User toUser = isPassenger ? trip.getDriver().getUser() : trip.getPassenger();

        Rating rating = Rating.builder()
                .trip(trip)
                .fromUser(fromUser)
                .toUser(toUser)
                .direction(direction)
                .score(req.score())
                .comment(req.comment())
                .build();
        rating = ratingRepository.save(rating);

        if (isPassenger) {
            updateDriverAverage(trip.getDriver().getId(), req.score());
            if (req.tip() != null && req.tip().signum() > 0) {
                trip.setTipAmount(req.tip());
                tripRepository.save(trip);
            }
        } else {
            updatePassengerAverage(trip.getPassenger().getId(), req.score());
        }
        return rating;
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsForUser(Long userId) {
        return ratingRepository.findByToUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(RatingResponse::from)
                .toList();
    }

    /**
     * Calificaciones recibidas por un operador especifico, para que el admin (o super admin) las
     * vea junto con el comentario; valida que el operador pertenezca a la organizacion en turno.
     */
    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsForDriver(DriverProfile driver, Organization scopedOrg) {
        if (scopedOrg != null && !driver.getOrganization().getId().equals(scopedOrg.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El operador no pertenece a tu organizacion");
        }
        return getRatingsForUser(driver.getUser().getId());
    }

    private void updateDriverAverage(Long driverId, int score) {
        DriverProfile driver = driverProfileRepository.findById(driverId).orElseThrow();
        BigDecimal total = driver.getRatingAvg().multiply(BigDecimal.valueOf(driver.getRatingCount()))
                .add(BigDecimal.valueOf(score));
        int newCount = driver.getRatingCount() + 1;
        driver.setRatingAvg(total.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        driver.setRatingCount(newCount);
        driverProfileRepository.save(driver);
    }

    private void updatePassengerAverage(Long userId, int score) {
        PassengerProfile profile = passengerProfileRepository.findByUser_Id(userId).orElseThrow();
        BigDecimal total = profile.getRatingAvg().multiply(BigDecimal.valueOf(profile.getRatingCount()))
                .add(BigDecimal.valueOf(score));
        int newCount = profile.getRatingCount() + 1;
        profile.setRatingAvg(total.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        profile.setRatingCount(newCount);
        passengerProfileRepository.save(profile);
    }
}
