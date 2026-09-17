package com.irp.incident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.incident.api.dto.VerificationRequest;
import com.irp.incident.domain.IllegalIncidentTransitionException;
import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.RemediationExecution;
import com.irp.incident.domain.Severity;
import com.irp.incident.observability.IncidentMetrics;
import com.irp.incident.repository.IncidentRepository;
import com.irp.incident.repository.RemediationExecutionRepository;
import com.irp.incident.repository.VerificationRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:10:00Z");

    @Mock IncidentRepository incidents;
    @Mock IncidentService incidentService;
    @Mock RemediationExecutionRepository executions;
    @Mock VerificationRunRepository runs;
    @Mock IncidentMetrics metrics;

    private VerificationService service;

    @BeforeEach
    void setUp() {
        service = new VerificationService(
                incidents,
                incidentService,
                executions,
                runs,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                metrics,
                2
        );
    }

    @Test
    void successfulExecutionAndResolvedOutcomeClosesTheIncident() {
        UUID id = UUID.randomUUID();
        Incident incident = verifyingIncident(id);
        when(incidents.findById(id)).thenReturn(Optional.of(incident));
        when(executions.findByIncidentIdOrderByStartedAtDesc(id)).thenReturn(List.of(succeeded(id)));

        service.record(id, new VerificationRequest("RESOLVED", "error rate recovered", List.of("overlay: SUCCEEDED")));

        assertThat(incident.getVerificationAttempts()).isEqualTo(1);
        verify(incidentService).resolve(eq(id), eq("verification-agent"), eq("error rate recovered"));
        verify(incidentService, never()).transition(any(), any(), any(), any(), any());
        verify(metrics).recordVerification("RESOLVED");
    }

    @Test
    void llmCannotResolveWithoutSuccessfulExecution() {
        UUID id = UUID.randomUUID();
        Incident incident = verifyingIncident(id);
        when(incidents.findById(id)).thenReturn(Optional.of(incident));
        when(executions.findByIncidentIdOrderByStartedAtDesc(id)).thenReturn(List.of());

        service.record(id, new VerificationRequest("RESOLVED", "looks fine", List.of()));

        verify(incidentService, never()).resolve(any(), any(), any());
        verify(incidentService).transition(
                eq(id),
                eq(IncidentStatus.INVESTIGATING),
                eq("VERIFICATION_RETRY"),
                eq("verification-agent"),
                any()
        );
        verify(metrics).recordVerification("NOT_RESOLVED");
    }

    @Test
    void exhaustedBudgetStaysInVerifyingAndEscalates() {
        UUID id = UUID.randomUUID();
        Incident incident = verifyingIncident(id);
        incident.incrementVerificationAttempts();
        when(incidents.findById(id)).thenReturn(Optional.of(incident));
        when(executions.findByIncidentIdOrderByStartedAtDesc(id)).thenReturn(List.of());

        service.record(id, new VerificationRequest("NOT_RESOLVED", "still failing", List.of()));

        assertThat(incident.getVerificationAttempts()).isEqualTo(2);
        verify(incidentService, never()).transition(any(), any(), any(), any(), any());
        verify(incidentService).recordAudit(
                eq(id),
                eq("VERIFICATION_ESCALATED"),
                eq("verification-agent"),
                any()
        );
        verify(metrics).recordVerificationEscalated();
    }

    @Test
    void rejectsVerificationOutsideVerifying() {
        UUID id = UUID.randomUUID();
        Incident incident = new Incident(
                id, "ext", "payment-service", "title", "desc",
                Severity.SEV2, "prod", NOW);
        when(incidents.findById(id)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.record(id, new VerificationRequest("RESOLVED", "nope", List.of())))
                .isInstanceOf(IllegalIncidentTransitionException.class);
        verify(runs, never()).save(any());
    }

    private static Incident verifyingIncident(UUID id) {
        Incident incident = new Incident(
                id, "ext", "payment-service", "title", "desc",
                Severity.SEV2, "prod", Instant.parse("2026-01-01T00:00:00Z"));
        Instant t = Instant.parse("2026-01-01T00:05:00Z");
        incident.transitionTo(IncidentStatus.TRIAGING, t);
        incident.transitionTo(IncidentStatus.INVESTIGATING, t);
        incident.transitionTo(IncidentStatus.ROOT_CAUSE_IDENTIFIED, t);
        incident.transitionTo(IncidentStatus.REMEDIATION_PROPOSED, t);
        incident.transitionTo(IncidentStatus.AWAITING_APPROVAL, t);
        incident.transitionTo(IncidentStatus.REMEDIATING, t);
        incident.transitionTo(IncidentStatus.VERIFYING, t);
        return incident;
    }

    private static RemediationExecution succeeded(UUID incidentId) {
        return new RemediationExecution(
                UUID.randomUUID(),
                incidentId,
                UUID.randomUUID(),
                incidentId + ":ROLLBACK_DEPLOYMENT:prod:payment-service:41",
                "SUCCEEDED",
                "rolled back to 41",
                null,
                NOW,
                NOW
        );
    }
}
