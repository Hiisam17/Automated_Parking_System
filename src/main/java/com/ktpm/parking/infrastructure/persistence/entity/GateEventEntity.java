package com.ktpm.parking.infrastructure.persistence.entity;

import com.ktpm.parking.business.domain.GateEventProcessingStatus;
import com.ktpm.parking.business.domain.GateEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "gate_events")
public class GateEventEntity {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "gate_id", nullable = false, length = 64)
    private String gateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private GateEventType eventType;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private GateEventProcessingStatus processingStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "jsonb")
    private String responseData;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected GateEventEntity() {
    }

    public GateEventEntity(String eventId, String gateId, GateEventType eventType,
                           String requestHash, GateEventProcessingStatus processingStatus,
                           String responseData, OffsetDateTime createdAt) {
        this.eventId = eventId;
        this.gateId = gateId;
        this.eventType = eventType;
        this.requestHash = requestHash;
        this.processingStatus = processingStatus;
        this.responseData = responseData;
        this.createdAt = createdAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getGateId() {
        return gateId;
    }

    public GateEventType getEventType() {
        return eventType;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public GateEventProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getResponseData() {
        return responseData;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
