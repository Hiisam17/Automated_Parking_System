package com.ktpm.parking.business.domain;

import java.time.OffsetDateTime;

public record GateEvent(
        String eventId,
        String gateId,
        GateEventType eventType,
        String requestHash,
        GateEventProcessingStatus processingStatus,
        String responseData,
        OffsetDateTime createdAt
) {
}
