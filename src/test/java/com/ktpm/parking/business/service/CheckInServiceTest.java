package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.exception.BusinessException;
import com.ktpm.parking.business.repository.EventStart;
import com.ktpm.parking.business.repository.GateEventWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T08:30:00Z");

    @Mock
    ParkingSpotWorkflowRepository spotRepository;

    @Mock
    ParkingSessionWorkflowRepository sessionRepository;

    @Mock
    GateEventWorkflowRepository eventRepository;

    @Mock
    ParkingUnitOfWork unitOfWork;

    CheckInService service;

    @BeforeEach
    void setUp() {
        service = new CheckInService(
                spotRepository,
                sessionRepository,
                eventRepository,
                unitOfWork,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateActiveSessionAndOccupyFirstAvailableSpot() {
        executeSynchronously();
        CheckInCommand command = command("evt-001", "29a - 12345");
        ParkingSpot spot = new ParkingSpot(
                5L, "A05", VehicleType.MOTORBIKE, ParkingSpotStatus.AVAILABLE, false
        );
        ParkingSession session = new ParkingSession(
                101L,
                "29A - 12345",
                VehicleType.MOTORBIKE,
                5L,
                "A05",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                null,
                ParkingSessionStatus.ACTIVE
        );

        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.existsActiveByLicensePlate("29A12345")).thenReturn(false);
        when(spotRepository.lockFirstAvailable(VehicleType.MOTORBIKE))
                .thenReturn(Optional.of(spot));
        when(spotRepository.markOccupied(5L)).thenReturn(true);
        when(sessionRepository.createActive(
                "29A - 12345",
                "29A12345",
                VehicleType.MOTORBIKE,
                5L,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        )).thenReturn(session);

        GateResult result = service.checkIn(command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.decision().accepted()).isTrue();
        assertThat(result.decision().sessionId()).isEqualTo(101L);
        assertThat(result.decision().spotCode()).isEqualTo("A05");
        verify(eventRepository).saveResult("evt-001", result.decision());
    }

    @Test
    void shouldRejectVehicleThatAlreadyHasActiveSession() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.existsActiveByLicensePlate("29A12345")).thenReturn(true);

        GateResult result = service.checkIn(command("evt-002", "29A-12345"));

        assertThat(result.decision().accepted()).isFalse();
        assertThat(result.decision().code()).isEqualTo("VEHICLE_ALREADY_PARKED");
        verify(eventRepository).saveResult("evt-002", result.decision());
        verifyNoInteractions(spotRepository);
    }

    @Test
    void shouldRejectWhenNoCompatibleSpotIsAvailable() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.existsActiveByLicensePlate("29A12345")).thenReturn(false);
        when(spotRepository.lockFirstAvailable(VehicleType.MOTORBIKE))
                .thenReturn(Optional.empty());

        GateResult result = service.checkIn(command("evt-003", "29A-12345"));

        assertThat(result.decision().code()).isEqualTo("NO_AVAILABLE_SPOT");
        verify(eventRepository).saveResult("evt-003", result.decision());
        verify(sessionRepository, never()).createActive(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldReplayPreviousDecisionWithoutApplyingSideEffects() {
        executeSynchronously();
        GateDecision previous = GateDecision.accepted(101L, "A05");
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.Replay(previous));

        GateResult result = service.checkIn(command("evt-004", "29A-12345"));

        assertThat(result.replayed()).isTrue();
        assertThat(result.decision()).isSameAs(previous);
        verifyNoInteractions(sessionRepository, spotRepository);
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldRejectReusedEventIdWithoutOverwritingStoredResult() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.Reused());

        GateResult result = service.checkIn(command("evt-005", "29A-12345"));

        assertThat(result.decision().code()).isEqualTo("EVENT_ID_REUSED");
        verifyNoInteractions(sessionRepository, spotRepository);
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldFailTransactionWhenLockedSpotCannotBeOccupied() {
        executeSynchronously();
        ParkingSpot spot = new ParkingSpot(
                5L, "A05", VehicleType.MOTORBIKE, ParkingSpotStatus.AVAILABLE, false
        );
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.existsActiveByLicensePlate("29A12345")).thenReturn(false);
        when(spotRepository.lockFirstAvailable(VehicleType.MOTORBIKE))
                .thenReturn(Optional.of(spot));
        when(spotRepository.markOccupied(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.checkIn(command("evt-006", "29A-12345")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("marked occupied");

        verify(sessionRepository, never()).createActive(anyString(), anyString(), any(), any(), any());
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldRejectInvalidCommandBeforeStartingTransaction() {
        assertThatThrownBy(() -> service.checkIn(
                new CheckInCommand(" ", "IN-01", "29A-12345", VehicleType.MOTORBIKE)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR")
        );

        verifyNoInteractions(unitOfWork, eventRepository, sessionRepository, spotRepository);
    }

    @Test
    void shouldHashNormalizedPlateAndEntryPayload() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), any(), anyString()))
                .thenReturn(new EventStart.Reused());

        service.checkIn(command("evt-007", "29a 12345"));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventRepository).beginOrReplay(
                org.mockito.ArgumentMatchers.eq("evt-007"),
                org.mockito.ArgumentMatchers.eq(com.ktpm.parking.business.domain.GateEventType.ENTRY),
                hashCaptor.capture()
        );
        assertThat(hashCaptor.getValue()).hasSize(64);
    }

    private CheckInCommand command(String eventId, String licensePlate) {
        return new CheckInCommand(eventId, "IN-01", licensePlate, VehicleType.MOTORBIKE);
    }

    private void executeSynchronously() {
        when(unitOfWork.execute(any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(0)).get()
        );
    }
}
