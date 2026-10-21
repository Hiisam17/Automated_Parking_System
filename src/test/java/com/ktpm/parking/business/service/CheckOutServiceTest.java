package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckOutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:45:00Z");

    @Mock
    ParkingSpotWorkflowRepository spotRepository;

    @Mock
    ParkingSessionWorkflowRepository sessionRepository;

    @Mock
    GateEventWorkflowRepository eventRepository;

    @Mock
    ParkingUnitOfWork unitOfWork;

    CheckOutService service;

    @BeforeEach
    void setUp() {
        service = new CheckOutService(
                spotRepository,
                sessionRepository,
                eventRepository,
                unitOfWork,
                Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"))
        );
    }

    @Test
    void shouldCompleteActiveSessionAndReleaseSpot() {
        executeSynchronously();
        ParkingSession session = activeSession();
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.lockActiveByLicensePlate("29A12345"))
                .thenReturn(Optional.of(session));
        when(sessionRepository.complete(101L, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(true);
        when(spotRepository.markAvailable(5L)).thenReturn(true);

        GateResult result = service.checkOut(command("evt-exit-001", "29a - 12345"));

        assertThat(result.replayed()).isFalse();
        assertThat(result.decision().accepted()).isTrue();
        assertThat(result.decision().sessionId()).isEqualTo(101L);
        assertThat(result.decision().spotCode()).isEqualTo("A05");
        verify(eventRepository).saveResult("evt-exit-001", result.decision());
    }

    @Test
    void shouldRejectWhenVehicleHasNoActiveSession() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.lockActiveByLicensePlate("29A12345"))
                .thenReturn(Optional.empty());

        GateResult result = service.checkOut(command("evt-exit-002", "29A-12345"));

        assertThat(result.decision().accepted()).isFalse();
        assertThat(result.decision().code()).isEqualTo("NO_ACTIVE_SESSION");
        verify(eventRepository).saveResult("evt-exit-002", result.decision());
        verifyNoInteractions(spotRepository);
    }

    @Test
    void shouldReplayPreviousDecisionWithoutCompletingAgain() {
        executeSynchronously();
        GateDecision previous = GateDecision.accepted(101L, "A05");
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.Replay(previous));

        GateResult result = service.checkOut(command("evt-exit-003", "29A-12345"));

        assertThat(result.replayed()).isTrue();
        assertThat(result.decision()).isSameAs(previous);
        verifyNoInteractions(sessionRepository, spotRepository);
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldRejectReusedEventIdWithoutOverwritingResult() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.Reused());

        GateResult result = service.checkOut(command("evt-exit-004", "29A-12345"));

        assertThat(result.decision().code()).isEqualTo("EVENT_ID_REUSED");
        verifyNoInteractions(sessionRepository, spotRepository);
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldFailTransactionWhenLockedSessionCannotBeCompleted() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.lockActiveByLicensePlate("29A12345"))
                .thenReturn(Optional.of(activeSession()));
        when(sessionRepository.complete(101L, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.checkOut(command("evt-exit-005", "29A-12345")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not be completed");

        verifyNoInteractions(spotRepository);
        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldFailTransactionWhenOccupiedSpotCannotBeReleased() {
        executeSynchronously();
        when(eventRepository.beginOrReplay(anyString(), eq(GateEventType.EXIT), anyString()))
                .thenReturn(new EventStart.New());
        when(sessionRepository.lockActiveByLicensePlate("29A12345"))
                .thenReturn(Optional.of(activeSession()));
        when(sessionRepository.complete(101L, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(true);
        when(spotRepository.markAvailable(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.checkOut(command("evt-exit-006", "29A-12345")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("marked available");

        verify(eventRepository, never()).saveResult(anyString(), any());
    }

    @Test
    void shouldRejectInvalidCommandBeforeStartingTransaction() {
        assertThatThrownBy(() -> service.checkOut(
                new CheckOutCommand("evt-exit-007", " ", "29A-12345")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR")
        );

        verifyNoInteractions(unitOfWork, eventRepository, sessionRepository, spotRepository);
    }

    @Test
    void shouldRejectLicensePlateWithoutAlphanumericCharacters() {
        assertThatThrownBy(() -> service.checkOut(command("evt-exit-008", "----")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR")
                );

        verifyNoInteractions(unitOfWork, eventRepository, sessionRepository, spotRepository);
    }

    private ParkingSession activeSession() {
        return new ParkingSession(
                101L,
                "29A-12345",
                VehicleType.MOTORBIKE,
                5L,
                "A05",
                OffsetDateTime.parse("2026-10-21T08:30:00Z"),
                null,
                ParkingSessionStatus.ACTIVE
        );
    }

    private CheckOutCommand command(String eventId, String licensePlate) {
        return new CheckOutCommand(eventId, "OUT-01", licensePlate);
    }

    private void executeSynchronously() {
        when(unitOfWork.execute(any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(0)).get()
        );
    }
}
