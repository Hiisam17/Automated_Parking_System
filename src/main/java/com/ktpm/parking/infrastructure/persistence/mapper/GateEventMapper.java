package com.ktpm.parking.infrastructure.persistence.mapper;

import com.ktpm.parking.business.domain.GateEvent;
import com.ktpm.parking.infrastructure.persistence.entity.GateEventEntity;

public final class GateEventMapper {

    private GateEventMapper() {
    }

    public static GateEvent toDomain(GateEventEntity entity) {
        return new GateEvent(
                entity.getEventId(),
                entity.getGateId(),
                entity.getEventType(),
                entity.getRequestHash(),
                entity.getProcessingStatus(),
                entity.getResponseData(),
                entity.getCreatedAt()
        );
    }

    public static GateEventEntity toEntity(GateEvent domain) {
        return new GateEventEntity(
                domain.eventId(),
                domain.gateId(),
                domain.eventType(),
                domain.requestHash(),
                domain.processingStatus(),
                domain.responseData(),
                domain.createdAt()
        );
    }
}
