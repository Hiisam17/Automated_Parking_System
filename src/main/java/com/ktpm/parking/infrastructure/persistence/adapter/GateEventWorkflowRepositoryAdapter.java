package com.ktpm.parking.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.repository.EventStart;
import com.ktpm.parking.business.service.GateDecision;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class GateEventWorkflowRepositoryAdapter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GateEventWorkflowRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public EventStart beginOrReplay(String eventId, String gateId, GateEventType eventType, String requestHash) {
        int inserted = jdbc.update("""
                INSERT INTO gate_events (event_id, gate_id, event_type, request_hash, processing_status, created_at)
                VALUES (?, ?, ?, ?, 'PROCESSING', ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, gateId, eventType.name(), requestHash,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        if (inserted == 1) {
            return new EventStart.New();
        }

        // A competing INSERT waits for the owner to commit/rollback. A separate
        // READ_COMMITTED statement then sees its final result without poisoning
        // the transaction with a caught unique-constraint exception.
        return jdbc.queryForObject("""
                SELECT gate_id, event_type, request_hash, processing_status, response_data::text
                FROM gate_events WHERE event_id = ? FOR UPDATE
                """, (rs, row) -> {
            if (!gateId.equals(rs.getString("gate_id"))
                    || !eventType.name().equals(rs.getString("event_type"))
                    || !requestHash.equals(rs.getString("request_hash"))) {
                return new EventStart.Reused();
            }
            String status = rs.getString("processing_status");
            String response = rs.getString("response_data");
            if ("PROCESSING".equals(status) || response == null) {
                throw new DataIntegrityViolationException("Gate event has no committed decision: " + eventId);
            }
            try {
                GateDecision decision = objectMapper.readValue(response, GateDecision.class);
                if (decision == null || decision.accepted() != "SUCCEEDED".equals(status)) {
                    throw new DataIntegrityViolationException("Gate event decision does not match its status");
                }
                return new EventStart.Replay(decision);
            } catch (JsonProcessingException exception) {
                throw new DataRetrievalFailureException("Cannot read stored gate decision", exception);
            }
        }, eventId);
    }

    public void saveResult(String eventId, GateDecision result) {
        try {
            int updated = jdbc.update("""
                    UPDATE gate_events SET processing_status = ?, response_data = CAST(? AS jsonb)
                    WHERE event_id = ? AND processing_status = 'PROCESSING'
                    """, result.accepted() ? "SUCCEEDED" : "REJECTED",
                    objectMapper.writeValueAsString(result), eventId);
            if (updated != 1) {
                throw new DataIntegrityViolationException("Gate event is missing or already finalized: " + eventId);
            }
        } catch (JsonProcessingException exception) {
            throw new DataIntegrityViolationException("Cannot serialize gate decision", exception);
        }
    }
}
