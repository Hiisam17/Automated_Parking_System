package com.ktpm.parking.infrastructure.persistence;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.repository.EventStart;
import com.ktpm.parking.business.repository.ParkingSessionRepository;
import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import com.ktpm.parking.business.service.CheckInCommand;
import com.ktpm.parking.business.service.CheckOutCommand;
import com.ktpm.parking.business.service.GateDecision;
import com.ktpm.parking.business.service.GateResult;
import com.ktpm.parking.business.service.ParkingService;
import com.ktpm.parking.business.support.GateRequestHasher;
import com.ktpm.parking.infrastructure.persistence.adapter.GateEventWorkflowRepositoryAdapter;
import com.ktpm.parking.infrastructure.transaction.PersistentParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(ParkingPersistenceIT.FixedClockConfiguration.class)
@Timeout(30)
class ParkingPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-10-25T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.gate-api-key", () -> "test-only-gate-api-key");
        registry.add("app.security.jwt-secret", () -> "test-only-jwt-secret-at-least-32-chars");
        registry.add("app.admin-bootstrap.username", () -> "");
        registry.add("app.admin-bootstrap.password", () -> "");
    }

    @Autowired ParkingService parking;
    @Autowired ParkingUnitOfWork unitOfWork;
    @Autowired ParkingSpotWorkflowRepository spots;
    @Autowired ParkingSessionWorkflowRepository sessions;
    @Autowired ParkingSessionRepository sessionCrud;
    @Autowired GateEventWorkflowRepositoryAdapter events;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE gate_events, parking_sessions RESTART IDENTITY");
        jdbc.update("UPDATE parking_spots SET occupancy_status = 'AVAILABLE', in_maintenance = FALSE");
    }

    @Test
    void entryAndExitCommitSessionSpotAndAuditWithUtcTimes() {
        GateResult entry = parking.checkIn(entry("entry", "29a - 12345"));

        assertThat(entry.decision().accepted()).isTrue();
        assertThat(entry.decision().spotCode()).isEqualTo("A01");
        assertThat(entry.replayed()).isFalse();
        assertThat(text("SELECT license_plate_key FROM parking_sessions")).isEqualTo("29A12345");
        assertThat(text("SELECT gate_id FROM gate_events")).isEqualTo("IN-01");
        assertThat(text("SELECT processing_status FROM gate_events")).isEqualTo("SUCCEEDED");
        assertThat(text("SELECT occupancy_status FROM parking_spots WHERE code = 'A01'"))
                .isEqualTo("OCCUPIED");

        // Mapping must finish inside the adapter's transaction with open-in-view disabled.
        ParkingSession saved = sessionCrud.findById(entry.decision().sessionId()).orElseThrow();
        assertThat(saved.licensePlate()).isEqualTo("29A - 12345");
        assertThat(saved.parkingSpotCode()).isEqualTo("A01");
        assertThat(saved.checkInTime()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        GateResult exit = parking.checkOut(exit("exit", "29a 12345"));
        assertThat(exit.decision()).isEqualTo(entry.decision());
        ParkingSession completed = sessionCrud.findById(saved.id()).orElseThrow();
        assertThat(completed.status()).isEqualTo(ParkingSessionStatus.COMPLETED);
        assertThat(completed.checkOutTime()).isEqualTo(saved.checkInTime());
        assertThat(text("SELECT occupancy_status FROM parking_spots WHERE code = 'A01'"))
                .isEqualTo("AVAILABLE");
        assertThat(text("SELECT gate_id FROM gate_events WHERE event_id = 'exit'")).isEqualTo("OUT-01");
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isEqualTo(2);
    }

    @Test
    void crudSaveDerivesTheSameLicensePlateKey() {
        ParkingSession saved = sessionCrud.save(new ParkingSession(null, "29a - 12345",
                VehicleType.MOTORBIKE, spotId("A01"), "A01",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), null, ParkingSessionStatus.ACTIVE));
        assertThat(saved.id()).isNotNull();
        assertThat(saved.parkingSpotCode()).isEqualTo("A01");
        assertThat(text("SELECT license_plate_key FROM parking_sessions")).isEqualTo("29A12345");
    }

    @Test
    void allocationSkipsMaintenanceAndOtherVehicleTypesInCodeOrder() {
        jdbc.update("UPDATE parking_spots SET in_maintenance = TRUE WHERE code = 'A01'");
        assertThat(parking.checkIn(entry("entry", "29A-12345")).decision().spotCode()).isEqualTo("A02");
        assertThat(parking.checkIn(new CheckInCommand("car", "IN-02", "30A-12345", VehicleType.CAR))
                .decision().spotCode()).isEqualTo("B01");
        assertThat(text("SELECT occupancy_status FROM parking_spots WHERE code = 'A01'"))
                .isEqualTo("AVAILABLE");
    }

    @Test
    void acceptedEventsReplayTheirOriginalDecisionAfterStateChanges() {
        GateResult first = parking.checkIn(entry("entry", "29A-12345"));
        GateResult exit = parking.checkOut(exit("exit", "29A12345"));
        parking.checkIn(entry("next-entry", "29A12345"));

        GateResult replayEntry = parking.checkIn(entry("entry", "29a 12345"));
        GateResult replayExit = parking.checkOut(exit("exit", "29a - 12345"));
        assertThat(replayEntry.replayed()).isTrue();
        assertThat(replayEntry.decision()).isEqualTo(first.decision());
        assertThat(replayExit.replayed()).isTrue();
        assertThat(replayExit.decision()).isEqualTo(exit.decision());
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM parking_sessions WHERE status = 'ACTIVE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isEqualTo(3);
    }

    @Test
    void rejectedCapacityAndMissingSessionDecisionsRemainReplayable() {
        jdbc.update("UPDATE parking_spots SET in_maintenance = TRUE WHERE vehicle_type = 'MOTORBIKE'");
        GateResult rejectedEntry = parking.checkIn(entry("full", "29A12345"));
        assertThat(rejectedEntry.decision().code()).isEqualTo("NO_AVAILABLE_SPOT");
        GateResult rejectedExit = parking.checkOut(exit("missing", "29A12345"));
        assertThat(rejectedExit.decision().code()).isEqualTo("NO_ACTIVE_SESSION");
        jdbc.update("UPDATE parking_spots SET in_maintenance = FALSE");
        parking.checkIn(entry("entry", "29A12345"));

        GateResult replayEntry = parking.checkIn(entry("full", "29A-12345"));
        GateResult replayExit = parking.checkOut(exit("missing", "29A-12345"));
        assertThat(replayEntry.replayed()).isTrue();
        assertThat(replayEntry.decision()).isEqualTo(rejectedEntry.decision());
        assertThat(replayExit.replayed()).isTrue();
        assertThat(replayExit.decision()).isEqualTo(rejectedExit.decision());
        assertThat(count("SELECT COUNT(*) FROM parking_sessions WHERE status = 'ACTIVE'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM gate_events WHERE processing_status = 'REJECTED'")).isEqualTo(2);
    }

    @Test
    void eventIdCannotBeReusedForAnotherPayloadGateOrEventType() {
        GateResult original = parking.checkIn(entry("same", "29A12345"));
        assertThat(parking.checkIn(entry("same", "30A12345")).decision().code()).isEqualTo("EVENT_ID_REUSED");
        assertThat(parking.checkIn(new CheckInCommand("same", "IN-02", "29A12345", VehicleType.MOTORBIKE))
                .decision().code()).isEqualTo("EVENT_ID_REUSED");
        assertThat(parking.checkOut(exit("same", "29A12345")).decision().code()).isEqualTo("EVENT_ID_REUSED");
        assertThat(parking.checkIn(entry("same", "29A12345")).decision()).isEqualTo(original.decision());
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(1);
    }

    @Test
    void failureAfterOccupyingSpotRollsBackEventAndSpotAndAllowsRetry() {
        ParkingSessionWorkflowRepository failing = mock(ParkingSessionWorkflowRepository.class, delegatesTo(sessions));
        doThrow(new IllegalStateException("injected session failure")).when(failing)
                .createActive(anyString(), anyString(), any(), any(), any());
        ParkingService service = service(spots, failing, events);

        assertThatThrownBy(() -> service.checkIn(entry("retry", "29A12345")))
                .isInstanceOf(IllegalStateException.class).hasMessage("injected session failure");
        assertEmptyWorkflow();
        assertThat(parking.checkIn(entry("retry", "29A12345")).decision().accepted()).isTrue();
    }

    @Test
    void failureSavingDecisionRollsBackCreatedSessionAndAllOtherChanges() {
        GateEventWorkflowRepositoryAdapter failing = mock(GateEventWorkflowRepositoryAdapter.class, delegatesTo(events));
        doThrow(new IllegalStateException("injected audit failure")).when(failing).saveResult(anyString(), any());

        assertThatThrownBy(() -> service(spots, sessions, failing).checkIn(entry("retry", "29A12345")))
                .isInstanceOf(IllegalStateException.class).hasMessage("injected audit failure");
        assertEmptyWorkflow();
        assertThat(parking.checkIn(entry("retry", "29A12345")).replayed()).isFalse();
    }

    @Test
    void aRealSqlConstraintFailureRollsBackBothJpaAndJdbcWrites() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            events.beginOrReplay("failed", "IN-01", GateEventType.ENTRY, "hash");
            spots.markOccupied(spotId("A01"));
            sessionCrud.save(new ParkingSession(null, "29A-12345", VehicleType.MOTORBIKE,
                    spotId("A01"), "A01", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                    null, ParkingSessionStatus.ACTIVE));
            // Deliberately violate the ACTIVE spot index through JDBC after a JPA insert.
            insertSession("30A12345", "30A12345", spotId("A01"), "ACTIVE", null);
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_parking_sessions_active_spot");
        assertEmptyWorkflow();
        assertThat(parking.checkIn(entry("failed", "29A-12345")).decision().accepted()).isTrue();
    }

    @Test
    void failedSpotReleaseRollsBackCheckoutAndAllowsTheSameEventToRetry() {
        GateResult entry = parking.checkIn(entry("entry", "29A12345"));
        ParkingSpotWorkflowRepository failing = mock(ParkingSpotWorkflowRepository.class, delegatesTo(spots));
        doReturn(false).when(failing).markAvailable(any());

        assertThatThrownBy(() -> service(failing, sessions, events).checkOut(exit("retry", "29A12345")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("marked available");
        assertThat(sessionCrud.findById(entry.decision().sessionId()).orElseThrow().status())
                .isEqualTo(ParkingSessionStatus.ACTIVE);
        assertThat(text("SELECT occupancy_status FROM parking_spots WHERE code = 'A01'"))
                .isEqualTo("OCCUPIED");
        assertThat(count("SELECT COUNT(*) FROM gate_events WHERE event_id = 'retry'")).isZero();
        assertThat(parking.checkOut(exit("retry", "29A12345")).decision().accepted()).isTrue();
    }

    @Test
    void workflowOperationsRequireAnExistingTransaction() {
        assertThatThrownBy(() -> spots.lockFirstAvailable(VehicleType.MOTORBIKE))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> sessions.existsActiveByLicensePlate("29A12345"))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> events.beginOrReplay("event", "IN-01", GateEventType.ENTRY, "hash"))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertEmptyWorkflow();
    }

    @Test
    void unitOfWorkRejectsIncompatibleOuterIsolationAndReadOnlyTransactions() {
        TransactionTemplate serializable = new TransactionTemplate(transactionManager);
        serializable.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        assertThatThrownBy(() -> serializable.execute(status -> unitOfWork.execute(() -> 1)))
                .isInstanceOf(IllegalTransactionStateException.class);
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        assertThatThrownBy(() -> readOnly.execute(status -> unitOfWork.execute(() -> 1)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void conditionalUpdatesOnlyAcceptTheExpectedState() {
        unitOfWork.execute(() -> {
            Long id = spotId("A01");
            assertThat(spots.markAvailable(id)).isFalse();
            assertThat(spots.markOccupied(id)).isTrue();
            assertThat(spots.markOccupied(id)).isFalse();
            assertThat(spots.markAvailable(id)).isTrue();
            jdbc.update("UPDATE parking_spots SET in_maintenance = TRUE WHERE id = ?", id);
            assertThat(spots.markOccupied(id)).isFalse();
            assertThat(sessions.complete(-1L, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))).isFalse();
            return null;
        });
    }

    @Test
    void databaseRejectsDuplicateActivePlatesAndSpotsIndependently() {
        parking.checkIn(entry("entry", "29A12345"));
        assertThatThrownBy(() -> insertSession("29a - 12345", "29A12345", spotId("A02"), "ACTIVE", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_parking_sessions_active_license_plate_key");
        assertThatThrownBy(() -> insertSession("30A12345", "30A12345", spotId("A01"), "ACTIVE", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_parking_sessions_active_spot");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "'DIFFERENT'", "''", "'29a12345'"})
    void databaseRejectsMissingInvalidOrMismatchedLicensePlateKeys(String keySql) {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO parking_sessions
                    (license_plate, license_plate_key, vehicle_type, parking_spot_id, check_in_time, status)
                VALUES ('29A-12345', %s, 'MOTORBIKE', ?, CURRENT_TIMESTAMP, 'ACTIVE')
                """.formatted(keySql), spotId("A01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseEnforcesSessionTimelineCompletionAndForeignKey() {
        assertThatThrownBy(() -> insertSession("29A12345", "29A12345", spotId("A01"), "COMPLETED", null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("completion");
        assertThatThrownBy(() -> insertSession("29A12345", "29A12345", spotId("A01"), "ACTIVE", "2026-10-25T09:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("completion");
        assertThatThrownBy(() -> insertSession("29A12345", "29A12345", spotId("A01"), "COMPLETED", "2026-10-24T08:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("timeline");
        assertThatThrownBy(() -> insertSession("29A12345", "29A12345", -1L, "ACTIVE", null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_parking_sessions_spot");
    }

    @Test
    void simultaneousEntriesCompetingForOneSpotAllocateItOnlyOnce() throws Exception {
        leaveOnlyA01Available();
        Race<GateResult, GateResult> result = race(
                () -> parking.checkIn(entry("first", "29A12345")),
                () -> parking.checkIn(entry("second", "30A12345")));
        assertThat(result.first().decision().accepted() ^ result.second().decision().accepted()).isTrue();
        GateResult rejected = result.first().decision().accepted() ? result.second() : result.first();
        assertThat(rejected.decision().code()).isEqualTo("NO_AVAILABLE_SPOT");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM parking_spots WHERE occupancy_status = 'OCCUPIED'")).isEqualTo(1);
    }

    @Test
    void simultaneousEntriesForDifferentPlatesUseDifferentSpotsInCodeOrder() throws Exception {
        Race<GateResult, GateResult> result = race(
                () -> parking.checkIn(entry("first", "29A12345")),
                () -> parking.checkIn(entry("second", "30A12345")));
        assertThat(result.first().decision().accepted()).isTrue();
        assertThat(result.second().decision().accepted()).isTrue();
        assertThat(java.util.List.of(result.first().decision().spotCode(), result.second().decision().spotCode()))
                .containsExactlyInAnyOrder("A01", "A02");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(2);
    }

    @Test
    void simultaneousEntriesForTheSameNormalizedPlateProduceOneSessionAndOneBusinessRejection() throws Exception {
        Race<GateResult, GateResult> result = race(
                () -> parking.checkIn(entry("first", "29A-12345")),
                () -> parking.checkIn(entry("second", "29a 12345")));
        assertThat(result.first().decision().accepted() ^ result.second().decision().accepted()).isTrue();
        GateResult rejected = result.first().decision().accepted() ? result.second() : result.first();
        assertThat(rejected.decision().code()).isEqualTo("VEHICLE_ALREADY_PARKED");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isEqualTo(2);
    }

    @Test
    void simultaneousExitsCompleteTheSessionOnlyOnce() throws Exception {
        parking.checkIn(entry("entry", "29A12345"));
        Race<GateResult, GateResult> result = race(
                () -> parking.checkOut(exit("first", "29A-12345")),
                () -> parking.checkOut(exit("second", "29a 12345")));
        assertThat(result.first().decision().accepted() ^ result.second().decision().accepted()).isTrue();
        GateResult rejected = result.first().decision().accepted() ? result.second() : result.first();
        assertThat(rejected.decision().code()).isEqualTo("NO_ACTIVE_SESSION");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions WHERE status = 'COMPLETED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM parking_spots WHERE occupancy_status = 'OCCUPIED'")).isZero();
    }

    @Test
    void concurrentSameEventWaitsForCommitThenReplays() throws Exception {
        assertContendedEvent(false);
    }

    @Test
    void concurrentSameEventCanTakeOverAfterOwnerRollsBack() throws Exception {
        assertContendedEvent(true);
    }

    @Test
    void maintenanceConditionalUpdateAndEntryCannotBothClaimTheSpot() throws Exception {
        leaveOnlyA01Available();
        Race<GateResult, Boolean> result = race(
                () -> parking.checkIn(entry("entry", "29A12345")),
                () -> unitOfWork.execute(() -> jdbc.update("""
                        UPDATE parking_spots SET in_maintenance = TRUE
                        WHERE code = 'A01' AND occupancy_status = 'AVAILABLE' AND NOT in_maintenance
                        """) == 1));
        assertThat(result.first().decision().accepted() ^ result.second()).isTrue();
        assertThat(count("""
                SELECT COUNT(*) FROM parking_spots
                WHERE occupancy_status = 'OCCUPIED' AND in_maintenance
                """)).isZero();
    }

    @Test
    void incompleteOrCorruptStoredEventsFailWithoutCommittingNewSideEffects() {
        String hash = GateRequestHasher.forEntry("IN-01", "29A12345", VehicleType.MOTORBIKE);
        unitOfWork.execute(() -> events.beginOrReplay("event", "IN-01", GateEventType.ENTRY, hash));
        assertThatThrownBy(() -> parking.checkIn(entry("event", "29A12345")))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("no committed decision");
        jdbc.update("UPDATE gate_events SET processing_status = 'SUCCEEDED', response_data = '{}'::jsonb");
        assertThatThrownBy(() -> parking.checkIn(entry("event", "29A12345")))
                .isInstanceOf(DataRetrievalFailureException.class).hasMessageContaining("Cannot read stored gate decision");
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isZero();
        assertThat(count("SELECT COUNT(*) FROM parking_spots WHERE occupancy_status = 'OCCUPIED'")).isZero();
    }

    @Test
    void finalizedDecisionsCannotBeOverwritten() {
        parking.checkIn(entry("event", "29A12345"));
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            events.saveResult("event", GateDecision.rejected("TEST", "must not replace"));
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("already finalized");
        assertThat(parking.checkIn(entry("event", "29A12345")).decision().accepted()).isTrue();
    }

    private void assertContendedEvent(boolean failOwner) throws Exception {
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        GateEventWorkflowRepositoryAdapter paused = mock(GateEventWorkflowRepositoryAdapter.class, delegatesTo(events));
        doAnswer(invocation -> {
            EventStart start = events.beginOrReplay(invocation.getArgument(0), invocation.getArgument(1),
                    invocation.getArgument(2), invocation.getArgument(3));
            inserted.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            if (failOwner) {
                throw new IllegalStateException("owner rolled back");
            }
            return start;
        }).when(paused).beginOrReplay(anyString(), anyString(), any(), anyString());

        try (var executor = Executors.newFixedThreadPool(2)) {
            var owner = executor.submit(() -> service(spots, sessions, paused).checkIn(entry("same", "29A12345")));
            try {
                assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
                var contender = executor.submit(() -> parking.checkIn(entry("same", "29A-12345")));
                // Observe actual PostgreSQL lock contention before allowing the owner
                // to finish. This covers the upsert wait path deterministically.
                await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(count("""
                        SELECT COUNT(*) FROM pg_stat_activity
                        WHERE datname = current_database() AND wait_event_type = 'Lock'
                          AND query LIKE '%INSERT INTO gate_events%'
                        """)).isPositive());
                release.countDown();
                GateResult result = contender.get(10, TimeUnit.SECONDS);
                assertThat(result.decision().accepted()).isTrue();
                assertThat(result.replayed()).isEqualTo(!failOwner);
                if (failOwner) {
                    assertThatThrownBy(() -> owner.get(10, TimeUnit.SECONDS))
                            .hasCauseInstanceOf(IllegalStateException.class);
                } else {
                    assertThat(owner.get(10, TimeUnit.SECONDS).decision()).isEqualTo(result.decision());
                }
            } finally {
                release.countDown();
            }
        }
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isEqualTo(1);
    }

    private <A, B> Race<A, B> race(Callable<A> first, Callable<B> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return first.call(); });
            var b = executor.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return second.call(); });
            return new Race<>(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }

    private ParkingService service(ParkingSpotWorkflowRepository spotRepository,
                                   ParkingSessionWorkflowRepository sessionRepository,
                                   GateEventWorkflowRepositoryAdapter eventRepository) {
        return new PersistentParkingService(spotRepository, sessionRepository, eventRepository, unitOfWork, CLOCK);
    }

    private void insertSession(String plate, String key, Long spot, String status, String checkout) {
        jdbc.update("""
                INSERT INTO parking_sessions
                    (license_plate, license_plate_key, vehicle_type, parking_spot_id, check_in_time, check_out_time, status)
                VALUES (?, ?, 'MOTORBIKE', ?, ?, ?, ?)
                """, plate, key, spot, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                checkout == null ? null : OffsetDateTime.parse(checkout), status);
    }

    private void assertEmptyWorkflow() {
        assertThat(count("SELECT COUNT(*) FROM parking_sessions")).isZero();
        assertThat(count("SELECT COUNT(*) FROM gate_events")).isZero();
        assertThat(count("SELECT COUNT(*) FROM parking_spots WHERE occupancy_status = 'OCCUPIED'")).isZero();
    }

    private void leaveOnlyA01Available() {
        jdbc.update("UPDATE parking_spots SET in_maintenance = TRUE WHERE code <> 'A01'");
    }

    private Long spotId(String code) {
        return jdbc.queryForObject("SELECT id FROM parking_spots WHERE code = ?", Long.class, code);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private CheckInCommand entry(String eventId, String plate) {
        return new CheckInCommand(eventId, "IN-01", plate, VehicleType.MOTORBIKE);
    }

    private CheckOutCommand exit(String eventId, String plate) {
        return new CheckOutCommand(eventId, "OUT-01", plate);
    }

    private record Race<A, B>(A first, B second) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }
}
