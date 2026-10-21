package com.ktpm.parking.business.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GateDecisionTest {

    @Test
    void shouldRequireSessionAndSpotForAcceptedDecision() {
        assertThatThrownBy(() -> new GateDecision(true, "ACCEPTED", "ok", null, "A01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GateDecision(true, "ACCEPTED", "ok", 1L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepAcceptedCodeConsistentWithDecisionState() {
        assertThatThrownBy(() -> new GateDecision(true, "OTHER", "ok", 1L, "A01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GateDecision(false, "ACCEPTED", "not ok", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectSessionAndSpotDetailsOnRejectedDecision() {
        assertThatThrownBy(() -> new GateDecision(
                false,
                "NO_AVAILABLE_SPOT",
                "Không còn chỗ.",
                1L,
                "A01"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
