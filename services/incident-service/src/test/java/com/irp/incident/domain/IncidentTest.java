package com.irp.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentTest {

    private Incident newIncident() {
        return new Incident(
                UUID.randomUUID(),
                "ext-1",
                "payment-service",
                "DB connection exhaustion",
                "Pool saturated",
                Severity.SEV2,
                "prod",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    @Test
    void newIncidentStartsInDetected() {
        assertThat(newIncident().getStatus()).isEqualTo(IncidentStatus.DETECTED);
    }

    @Test
    void transitionToResolvedSetsResolvedAt() {
        Incident incident = newIncident();
        Instant t = Instant.parse("2026-01-01T00:05:00Z");
        incident.transitionTo(IncidentStatus.TRIAGING, t);
        incident.transitionTo(IncidentStatus.INVESTIGATING, t);
        incident.transitionTo(IncidentStatus.ROOT_CAUSE_IDENTIFIED, t);
        incident.transitionTo(IncidentStatus.REMEDIATION_PROPOSED, t);
        incident.transitionTo(IncidentStatus.AWAITING_APPROVAL, t);
        incident.transitionTo(IncidentStatus.REMEDIATING, t);
        incident.transitionTo(IncidentStatus.VERIFYING, t);
        Instant resolvedAt = Instant.parse("2026-01-01T00:10:00Z");
        incident.transitionTo(IncidentStatus.RESOLVED, resolvedAt);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.getResolvedAt()).isEqualTo(resolvedAt);
    }

    @Test
    void illegalTransitionThrows() {
        Incident incident = newIncident();
        assertThatThrownBy(() -> incident.transitionTo(IncidentStatus.RESOLVED, Instant.now()))
                .isInstanceOf(IllegalIncidentTransitionException.class);
        // State remains unchanged after a rejected transition.
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.DETECTED);
    }

    @Test
    void nonTerminalTransitionsDoNotSetResolvedAt() {
        Incident incident = newIncident();
        incident.transitionTo(IncidentStatus.TRIAGING, Instant.now());
        assertThat(incident.getResolvedAt()).isNull();
    }

    @Test
    void verificationAttemptsStartAtZeroAndIncrement() {
        Incident incident = newIncident();
        assertThat(incident.getVerificationAttempts()).isZero();
        assertThat(incident.incrementVerificationAttempts()).isEqualTo(1);
        assertThat(incident.incrementVerificationAttempts()).isEqualTo(2);
    }
}
