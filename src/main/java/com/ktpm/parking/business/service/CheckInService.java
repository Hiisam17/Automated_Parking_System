package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSpot;
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

public final class CheckInService {

    private final ParkingSpotWorkflowRepository spotRepository;
    private final ParkingSessionWorkflowRepository sessionRepository;
    private final GateEventWorkflowRepository eventRepository;
    private final ParkingUnitOfWork unitOfWork;
    private final Clock clock;

    public CheckInService(
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

    public GateResult checkIn(CheckInCommand command) {
        GateCommandValidator.validate(command);

        String displayPlate = LicensePlateNormalizer.normalizeForDisplay(command.licensePlate());
        String licensePlateKey = LicensePlateNormalizer.toKey(command.licensePlate());
        String requestHash = GateRequestHasher.forEntry(
                command.gateId(),
                licensePlateKey,
                command.vehicleType()
        );

        return unitOfWork.execute(() -> process(
                command,
                displayPlate,
                licensePlateKey,
                requestHash
        ));
    }

    private GateResult process(
            CheckInCommand command,
            String displayPlate,
            String licensePlateKey,
            String requestHash
    ) {
        EventStart eventStart = eventRepository.beginOrReplay(
                command.eventId(),
                GateEventType.ENTRY,
                requestHash
        );

        if (eventStart instanceof EventStart.Replay replay) {
            return GateResult.replay(replay.previous());
        }
        if (eventStart instanceof EventStart.Reused) {
            return GateResult.newResult(GateDecision.rejected(ParkingError.EVENT_ID_REUSED));
        }

        if (sessionRepository.existsActiveByLicensePlate(licensePlateKey)) {
            return rejectAndSave(command.eventId(), ParkingError.VEHICLE_ALREADY_PARKED);
        }

        ParkingSpot spot = spotRepository.lockFirstAvailable(command.vehicleType())
                .orElse(null);
        if (spot == null) {
            return rejectAndSave(command.eventId(), ParkingError.NO_AVAILABLE_SPOT);
        }

        if (!spotRepository.markOccupied(spot.id())) {
            throw new IllegalStateException("Locked parking spot could not be marked occupied");
        }

        ParkingSession session = sessionRepository.createActive(
                displayPlate,
                licensePlateKey,
                command.vehicleType(),
                spot.id(),
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
        );
        GateDecision accepted = GateDecision.accepted(session.id(), spot.code());
        eventRepository.saveResult(command.eventId(), accepted);
        return GateResult.newResult(accepted);
    }

    private GateResult rejectAndSave(String eventId, ParkingError error) {
        GateDecision rejected = GateDecision.rejected(error);
        eventRepository.saveResult(eventId, rejected);
        return GateResult.newResult(rejected);
    }
}
