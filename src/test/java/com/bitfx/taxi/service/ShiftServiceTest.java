package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.admin.ShiftRequest;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.*;
import com.bitfx.taxi.repository.DriverProfileRepository;
import com.bitfx.taxi.repository.ShiftRepository;
import com.bitfx.taxi.repository.ShiftTemplateRepository;
import com.bitfx.taxi.repository.TaxiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private TaxiRepository taxiRepository;
    @Mock
    private DriverProfileRepository driverProfileRepository;
    @Mock
    private ShiftTemplateRepository shiftTemplateRepository;

    private ShiftService shiftService;

    private Organization org;
    private Taxi taxi;
    private DriverProfile previousDriver;
    private DriverProfile newDriver;
    private User admin;
    private ShiftTemplate morningTemplate;

    @BeforeEach
    void setUp() {
        shiftService = new ShiftService(shiftRepository, taxiRepository, driverProfileRepository, shiftTemplateRepository);

        org = Organization.builder().id(1L).name("Taxis Demo").build();
        taxi = Taxi.builder().id(10L).organization(org).unitNumber("001").active(true).build();
        previousDriver = DriverProfile.builder().id(100L).organization(org).active(true).build();
        newDriver = DriverProfile.builder().id(200L).organization(org).active(true).build();
        admin = User.builder().id(1L).role(Role.ADMIN).organization(org).build();
        morningTemplate = ShiftTemplate.builder().id(500L).organization(org).name("Turno Manana")
                .startTime(LocalTime.of(6, 0)).endTime(LocalTime.of(18, 0)).active(true).build();

        lenient().when(taxiRepository.findById(10L)).thenReturn(Optional.of(taxi));
        lenient().when(driverProfileRepository.findById(200L)).thenReturn(Optional.of(newDriver));
        lenient().when(shiftTemplateRepository.findById(500L)).thenReturn(Optional.of(morningTemplate));
        lenient().when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(shiftRepository.findByTaxiAndRevokedFalse(taxi)).thenReturn(List.of());
        lenient().when(shiftRepository.findByDriverAndRevokedFalse(newDriver)).thenReturn(List.of());
    }

    @Test
    void unTurnoFijoNuevoCierraElTurnoFijoAnteriorDeEseTaxi() {
        Shift previousShift = Shift.builder().id(1L).taxi(taxi).driver(previousDriver).temporary(false).revoked(false)
                .shiftTemplate(morningTemplate).startAt(LocalDateTime.now().minusDays(1)).build();
        when(shiftRepository.findByTaxiAndRevokedFalse(taxi)).thenReturn(List.of(previousShift));

        ShiftRequest req = new ShiftRequest(10L, 200L, null, null, false, 500L);
        Shift result = shiftService.assign(req, admin, org);

        assertTrue(previousShift.isRevoked(), "El turno fijo anterior del taxi debe quedar revocado");
        assertFalse(result.isRevoked());
        assertFalse(result.isTemporary());
        assertEquals(newDriver.getId(), result.getDriver().getId());
        assertEquals(morningTemplate.getId(), result.getShiftTemplate().getId());
    }

    @Test
    void unTurnoTemporalNecesitaFechaDeFin() {
        ShiftRequest req = new ShiftRequest(10L, 200L, LocalDateTime.now(), null, true, null);

        assertThrows(ApiException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void unTurnoFijoNoPuedeLlevarFechaDeFin() {
        ShiftRequest req = new ShiftRequest(10L, 200L, null, LocalDateTime.now().plusHours(1), false, 500L);

        assertThrows(ApiException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void unTurnoFijoNecesitaPlantilla() {
        ShiftRequest req = new ShiftRequest(10L, 200L, null, null, false, null);

        assertThrows(ApiException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void unTurnoFijoRechazaPlantillaDeOtraOrganizacion() {
        Organization otraOrg = Organization.builder().id(2L).name("Otra").build();
        ShiftTemplate plantillaDeOtraOrg = ShiftTemplate.builder().id(600L).organization(otraOrg).name("Nocturno")
                .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(6, 0)).active(true).build();
        when(shiftTemplateRepository.findById(600L)).thenReturn(Optional.of(plantillaDeOtraOrg));

        ShiftRequest req = new ShiftRequest(10L, 200L, null, null, false, 600L);

        assertThrows(ApiException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void unTurnoFijoRechazaPlantillaDesactivada() {
        morningTemplate.setActive(false);

        ShiftRequest req = new ShiftRequest(10L, 200L, null, null, false, 500L);

        assertThrows(ApiException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void unTurnoTemporalNuevoRevocaUnTemporalQueSeCruceEnElMismoTaxi() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(4);
        Shift existingTemp = Shift.builder().id(2L).taxi(taxi).driver(previousDriver).temporary(true).revoked(false)
                .startAt(start.minusHours(1)).endAt(start.plusHours(2)).build();
        when(shiftRepository.findByTaxiAndRevokedFalse(taxi)).thenReturn(List.of(existingTemp));

        ShiftRequest req = new ShiftRequest(10L, 200L, start, end, true, null);
        Shift result = shiftService.assign(req, admin, org);

        assertTrue(existingTemp.isRevoked(), "El turno temporal que se cruza en tiempo debe quedar revocado");
        assertTrue(result.isTemporary());
    }

    @Test
    void noPermiteAsignarTaxiOOperadorDeOtraOrganizacion() {
        Organization otraOrg = Organization.builder().id(2L).name("Otra").build();
        Taxi taxiDeOtraOrg = Taxi.builder().id(10L).organization(otraOrg).unitNumber("001").active(true).build();
        when(taxiRepository.findById(10L)).thenReturn(Optional.of(taxiDeOtraOrg));

        ShiftRequest req = new ShiftRequest(10L, 200L, null, null, false, 500L);

        assertThrows(RuntimeException.class, () -> shiftService.assign(req, admin, org));
    }

    @Test
    void resolveCurrentPrefiereElTurnoTemporalSobreElFijoMientrasEsteVigente() {
        LocalDateTime now = LocalDateTime.now();
        Shift fijo = Shift.builder().id(1L).taxi(taxi).driver(newDriver).temporary(false).revoked(false)
                .shiftTemplate(null).startAt(now.minusDays(10)).build();
        Shift temporal = Shift.builder().id(2L).taxi(taxi).driver(newDriver).temporary(true).revoked(false)
                .startAt(now.minusHours(1)).endAt(now.plusHours(1)).build();
        when(shiftRepository.findByDriverAndRevokedFalse(newDriver)).thenReturn(List.of(fijo, temporal));

        Optional<Shift> current = shiftService.resolveCurrentForDriver(newDriver);

        assertTrue(current.isPresent());
        assertEquals(temporal.getId(), current.get().getId());
    }

    @Test
    void resolveCurrentCaeAlFijoCuandoElTemporalYaExpiro() {
        LocalDateTime now = LocalDateTime.now();
        Shift fijo = Shift.builder().id(1L).taxi(taxi).driver(newDriver).temporary(false).revoked(false)
                .shiftTemplate(null).startAt(now.minusDays(10)).build();
        Shift temporalExpirado = Shift.builder().id(2L).taxi(taxi).driver(newDriver).temporary(true).revoked(false)
                .startAt(now.minusHours(5)).endAt(now.minusHours(1)).build();
        when(shiftRepository.findByDriverAndRevokedFalse(newDriver)).thenReturn(List.of(fijo, temporalExpirado));

        Optional<Shift> current = shiftService.resolveCurrentForDriver(newDriver);

        assertTrue(current.isPresent());
        assertEquals(fijo.getId(), current.get().getId());
    }

    @Test
    void resolveCurrentEsVacioSiNoHayNingunTurnoVigente() {
        when(shiftRepository.findByDriverAndRevokedFalse(newDriver)).thenReturn(List.of());

        assertTrue(shiftService.resolveCurrentForDriver(newDriver).isEmpty());
    }

    @Test
    void unTurnoFijoConPlantillaSoloEstaVigenteDentroDeSuHorario() {
        LocalDateTime now = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        Shift fijoDentroDeHorario = Shift.builder().id(1L).taxi(taxi).driver(newDriver).temporary(false).revoked(false)
                .shiftTemplate(morningTemplate).startAt(now.minusDays(1)).build();

        assertTrue(shiftService.isCurrentlyInEffect(fijoDentroDeHorario, now));
    }

    @Test
    void unTurnoFijoConPlantillaNoEstaVigenteFueraDeSuHorario() {
        LocalDateTime now = LocalDateTime.now().withHour(22).withMinute(0).withSecond(0).withNano(0);
        Shift fijoFueraDeHorario = Shift.builder().id(1L).taxi(taxi).driver(newDriver).temporary(false).revoked(false)
                .shiftTemplate(morningTemplate).startAt(now.minusDays(1)).build();

        assertFalse(shiftService.isCurrentlyInEffect(fijoFueraDeHorario, now));
    }

    @Test
    void ventanaDiariaQueCruzaMedianocheEsVigenteAntesYDespuesDeMedianoche() {
        ShiftTemplate nightTemplate = ShiftTemplate.builder().id(600L).organization(org).name("Turno Noche")
                .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(6, 0)).active(true).build();
        LocalDateTime now = LocalDateTime.now();
        Shift fijoNocturno = Shift.builder().id(3L).taxi(taxi).driver(newDriver).temporary(false).revoked(false)
                .shiftTemplate(nightTemplate).startAt(now.minusDays(1)).build();

        assertTrue(shiftService.isCurrentlyInEffect(fijoNocturno, now.withHour(23).withMinute(0)));
        assertTrue(shiftService.isCurrentlyInEffect(fijoNocturno, now.withHour(3).withMinute(0)));
        assertFalse(shiftService.isCurrentlyInEffect(fijoNocturno, now.withHour(12).withMinute(0)));
    }
}
