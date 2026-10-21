package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.repository.EventStart;
import com.ktpm.parking.business.repository.GateEventWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import com.ktpm.parking.business.support.GateRequestHasher;
import com.ktpm.parking.business.support.LicensePlateNormalizer;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public final class CheckOutService {

    private final ParkingSpotWorkflowRepository spotRepository;
    private final ParkingSessionWorkflowRepository sessionRepository;
    private final GateEventWorkflowRepository eventRepository;
    private final ParkingUnitOfWork unitOfWork;
    private final Clock clock;

    public CheckOutService(
            ParkingSpotWorkflowRepository spotRepository,
            ParkingSessionWorkflowRepository sessionRepository,
            GateEventWorkflowRepository eventRepository,
            ParkingUnitOfWork unitOfWork,
            Clock clock
    ) {
        this.spotRepository = Objects.requireNonNull(spotRepository);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    public GateResult checkOut(CheckOutCommand command) {
        GateCommandValidator.validate(command);

        String licensePlateKey = LicensePlateNormalizer.toKey(command.licensePlate());
        String requestHash = GateRequestHasher.forExit(command.gateId(), licensePlateKey);

        return unitOfWork.execute(() -> process(command, licensePlateKey, requestHash));
    }

    private GateResult process(
            CheckOutCommand command,
            String licensePlateKey,
            String requestHash
    ) {
        EventStart eventStart = eventRepository.beginOrReplay(
                command.eventId(),
                GateEventType.EXIT,
                requestHash
        );

        if (eventStart instanceof EventStart.Replay replay) {
            return GateResult.replay(replay.previous());
        }
        if (eventStart instanceof EventStart.Reused) {
            return GateResult.newResult(GateDecision.rejected(ParkingError.EVENT_ID_REUSED));
        }

        ParkingSession session = sessionRepository.lockActiveByLicensePlate(licensePlateKey)
                .orElse(null);
        if (session == null) {
            return rejectAndSave(command.eventId(), ParkingError.NO_ACTIVE_SESSION);
        }

        OffsetDateTime checkOutTime = OffsetDateTime.now(clock)
                .withOffsetSameInstant(ZoneOffset.UTC);
        if (!sessionRepository.complete(session.id(), checkOutTime)) {
            throw new IllegalStateException("Locked parking session could not be completed");
        }
        if (!spotRepository.markAvailable(session.parkingSpotId())) {
            throw new IllegalStateException("Occupied parking spot could not be marked available");
        }

        GateDecision accepted = GateDecision.accepted(session.id(), session.parkingSpotCode());
        eventRepository.saveResult(command.eventId(), accepted);
        return GateResult.newResult(accepted);
    }

    private GateResult rejectAndSave(String eventId, ParkingError error) {
        GateDecision rejected = GateDecision.rejected(error);
        eventRepository.saveResult(eventId, rejected);
        return GateResult.newResult(rejected);
    }
}
