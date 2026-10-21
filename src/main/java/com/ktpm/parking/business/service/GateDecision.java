package com.ktpm.parking.business.service;

import java.util.Objects;

public record GateDecision(
        boolean accepted,
        String code,
        String message,
        Long sessionId,
        String spotCode
) {

    public static final String ACCEPTED = "ACCEPTED";

    public GateDecision {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (accepted && (sessionId == null || spotCode == null || spotCode.isBlank())) {
            throw new IllegalArgumentException("Accepted decision requires sessionId and spotCode");
        }
        if (!accepted && (sessionId != null || spotCode != null)) {
            throw new IllegalArgumentException("Rejected decision must not contain session or spot");
        }
    }

    public static GateDecision accepted(Long sessionId, String spotCode) {
        return new GateDecision(true, ACCEPTED, "Yêu cầu đã được xử lý.", sessionId, spotCode);
    }

    public static GateDecision rejected(String code, String message) {
        return new GateDecision(false, code, message, null, null);
    }
}
