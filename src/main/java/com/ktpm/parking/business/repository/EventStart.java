package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.service.GateDecision;

import java.util.Objects;

public sealed interface EventStart permits EventStart.New, EventStart.Replay, EventStart.Reused {

    record New() implements EventStart {
    }

    record Replay(GateDecision previous) implements EventStart {
        public Replay {
            Objects.requireNonNull(previous, "previous decision must not be null");
        }
    }

    record Reused() implements EventStart {
    }
}
