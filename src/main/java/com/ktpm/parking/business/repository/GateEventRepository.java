package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.GateEvent;

import java.util.Optional;

public interface GateEventRepository {

    Optional<GateEvent> findByEventId(String eventId);

    GateEvent save(GateEvent gateEvent);
}
