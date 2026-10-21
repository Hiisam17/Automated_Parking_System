package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.service.GateDecision;

public interface GateEventWorkflowRepository {

    EventStart beginOrReplay(String eventId, GateEventType eventType, String requestHash);

    void saveResult(String eventId, GateDecision result);
}
