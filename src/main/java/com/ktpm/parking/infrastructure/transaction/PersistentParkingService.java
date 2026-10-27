package com.ktpm.parking.infrastructure.transaction;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.repository.EventStart;
import com.ktpm.parking.business.repository.GateEventWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import com.ktpm.parking.business.repository.ParkingUnitOfWork;
import com.ktpm.parking.business.service.CheckInCommand;
import com.ktpm.parking.business.service.CheckInService;
import com.ktpm.parking.business.service.CheckOutCommand;
import com.ktpm.parking.business.service.CheckOutService;
import com.ktpm.parking.business.service.GateDecision;
import com.ktpm.parking.business.service.GateResult;
import com.ktpm.parking.business.service.ParkingService;
import com.ktpm.parking.infrastructure.persistence.adapter.GateEventWorkflowRepositoryAdapter;

import java.time.Clock;

/** Connects the unchanged business ports to PostgreSQL, including gate audit metadata. */
public final class PersistentParkingService implements ParkingService {

    private final ParkingSpotWorkflowRepository spots;
    private final ParkingSessionWorkflowRepository sessions;
    private final GateEventWorkflowRepositoryAdapter events;
    private final ParkingUnitOfWork unitOfWork;
    private final Clock clock;

    public PersistentParkingService(ParkingSpotWorkflowRepository spots,
                                     ParkingSessionWorkflowRepository sessions,
                                     GateEventWorkflowRepositoryAdapter events,
                                     ParkingUnitOfWork unitOfWork, Clock clock) {
        this.spots = spots;
        this.sessions = sessions;
        this.events = events;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public GateResult checkIn(CheckInCommand command) {
        return new CheckInService(spots, sessions, eventsForGate(command == null ? null : command.gateId()),
                unitOfWork, clock).checkIn(command);
    }

    @Override
    public GateResult checkOut(CheckOutCommand command) {
        return new CheckOutService(spots, sessions, eventsForGate(command == null ? null : command.gateId()),
                unitOfWork, clock).checkOut(command);
    }

    private GateEventWorkflowRepository eventsForGate(String gateId) {
        // The existing business port omits gateId, but gate_events requires it.
        // Bind it immutably per invocation without changing Member 1's contract
        // or relying on servlet state / thread-local request metadata.
        return new GateEventWorkflowRepository() {
            @Override
            public EventStart beginOrReplay(String eventId, GateEventType eventType, String requestHash) {
                return events.beginOrReplay(eventId, gateId, eventType, requestHash);
            }

            @Override
            public void saveResult(String eventId, GateDecision result) {
                events.saveResult(eventId, result);
            }
        };
    }
}
