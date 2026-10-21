package com.ktpm.parking.business.service;

import java.util.Objects;

public record GateResult(GateDecision decision, boolean replayed) {

    public GateResult {
        Objects.requireNonNull(decision, "decision must not be null");
    }

    public static GateResult newResult(GateDecision decision) {
        return new GateResult(decision, false);
    }

    public static GateResult replay(GateDecision decision) {
        return new GateResult(decision, true);
    }
}
