package com.irp.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IncidentStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(IncidentStatus.DETECTED.canTransitionTo(IncidentStatus.TRIAGING)).isTrue();
        assertThat(IncidentStatus.TRIAGING.canTransitionTo(IncidentStatus.INVESTIGATING)).isTrue();
        assertThat(IncidentStatus.INVESTIGATING.canTransitionTo(IncidentStatus.ROOT_CAUSE_IDENTIFIED)).isTrue();
        assertThat(IncidentStatus.ROOT_CAUSE_IDENTIFIED.canTransitionTo(IncidentStatus.REMEDIATION_PROPOSED)).isTrue();
        assertThat(IncidentStatus.REMEDIATION_PROPOSED.canTransitionTo(IncidentStatus.AWAITING_APPROVAL)).isTrue();
        assertThat(IncidentStatus.REMEDIATION_PROPOSED.canTransitionTo(IncidentStatus.REMEDIATING)).isTrue();
        assertThat(IncidentStatus.AWAITING_APPROVAL.canTransitionTo(IncidentStatus.REMEDIATING)).isTrue();
        assertThat(IncidentStatus.REMEDIATING.canTransitionTo(IncidentStatus.VERIFYING)).isTrue();
        assertThat(IncidentStatus.VERIFYING.canTransitionTo(IncidentStatus.RESOLVED)).isTrue();
    }

    @Test
    void verificationFailureLoopsBackToInvestigating() {
        assertThat(IncidentStatus.VERIFYING.canTransitionTo(IncidentStatus.INVESTIGATING)).isTrue();
    }

    @Test
    void rejectionLoopsBackToInvestigating() {
        assertThat(IncidentStatus.AWAITING_APPROVAL.canTransitionTo(IncidentStatus.INVESTIGATING)).isTrue();
    }

    @Test
    void resolvedIsTerminal() {
        assertThat(IncidentStatus.RESOLVED.isTerminal()).isTrue();
        for (IncidentStatus s : IncidentStatus.values()) {
            assertThat(IncidentStatus.RESOLVED.canTransitionTo(s))
                    .as("RESOLVED must not transition to %s", s)
                    .isFalse();
        }
    }

    @Test
    void arbitraryJumpsAreDisallowed() {
        assertThat(IncidentStatus.DETECTED.canTransitionTo(IncidentStatus.RESOLVED)).isFalse();
        assertThat(IncidentStatus.DETECTED.canTransitionTo(IncidentStatus.REMEDIATING)).isFalse();
        assertThat(IncidentStatus.TRIAGING.canTransitionTo(IncidentStatus.AWAITING_APPROVAL)).isFalse();
        assertThat(IncidentStatus.INVESTIGATING.canTransitionTo(IncidentStatus.RESOLVED)).isFalse();
    }
}
