package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.trip.RatingResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.PassengerProfileRepository;
import com.bitfx.taxi.repository.RatingRepository;
import com.bitfx.taxi.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private PassengerProfileRepository passengerProfileRepository;

    private RatingService ratingService;

    private Organization org;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, tripRepository, driverProfileRepository, passengerProfileRepository);
        org = Organization.builder().id(1L).name("Taxis Demo").build();
    }

    @Test
    void getRatingsForUserIncluyeElComentarioYElNombreDeQuienCalifico() {
        User passenger = User.builder().id(10L).name("Pasajero Demo").build();
        User driverUser = User.builder().id(20L).name("Operador Demo").build();
        Trip trip = Trip.builder().id(100L).build();
        Rating rating = Rating.builder().id(1L).trip(trip).fromUser(passenger).toUser(driverUser)
                .direction(RatingDirection.PASSENGER_TO_DRIVER).score(5).comment("Excelente servicio")
                .createdAt(LocalDateTime.now()).build();
        when(ratingRepository.findByToUser_IdOrderByCreatedAtDesc(20L)).thenReturn(List.of(rating));

        List<RatingResponse> result = ratingService.getRatingsForUser(20L);

        assertEquals(1, result.size());
        assertEquals("Excelente servicio", result.get(0).comment());
        assertEquals("Pasajero Demo", result.get(0).fromUserName());
        assertEquals(5, result.get(0).score());
    }

    @Test
    void getRatingsForDriverRechazaSiElOperadorNoPerteneceALaOrganizacionEnTurno() {
        Organization otraOrg = Organization.builder().id(2L).name("Otra").build();
        DriverProfile driver = DriverProfile.builder().id(1L).organization(otraOrg)
                .user(User.builder().id(20L).name("Operador").build()).build();

        assertThrows(ApiException.class, () -> ratingService.getRatingsForDriver(driver, org));
        verify(ratingRepository, never()).findByToUser_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void getRatingsForDriverPermiteAlSuperAdminVerCualquierOrganizacion() {
        DriverProfile driver = DriverProfile.builder().id(1L).organization(org)
                .user(User.builder().id(20L).name("Operador").build()).build();
        when(ratingRepository.findByToUser_IdOrderByCreatedAtDesc(20L)).thenReturn(List.of());

        // scopedOrg null = super admin, sin restriccion de organizacion.
        assertDoesNotThrow(() -> ratingService.getRatingsForDriver(driver, null));
    }

    @Test
    void getRatingsForDriverPermiteAlAdminDeLaMismaOrganizacion() {
        DriverProfile driver = DriverProfile.builder().id(1L).organization(org)
                .user(User.builder().id(20L).name("Operador").build()).build();
        when(ratingRepository.findByToUser_IdOrderByCreatedAtDesc(20L)).thenReturn(List.of());

        assertDoesNotThrow(() -> ratingService.getRatingsForDriver(driver, org));
    }
}
