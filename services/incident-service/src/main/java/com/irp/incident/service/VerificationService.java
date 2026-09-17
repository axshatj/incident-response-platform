package com.irp.incident.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.incident.api.dto.VerificationRequest;
import com.irp.incident.api.dto.VerificationView;
import com.irp.incident.api.dto.VerificationView.RunView;
import com.irp.incident.domain.IllegalIncidentTransitionException;
import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentNotFoundException;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.VerificationRun;
import com.irp.incident.observability.IncidentMetrics;
import com.irp.incident.repository.IncidentRepository;
import com.irp.incident.repository.RemediationExecutionRepository;
import com.irp.incident.repository.VerificationRunRepository;
import com.irp.incident.verification.VerificationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    private final IncidentRepository incidents;
    private final IncidentService incidentService;
    private final RemediationExecutionRepository executions;
    private final VerificationRunRepository runs;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final IncidentMetrics metrics;
    private final int maxAttempts;

    public VerificationService(IncidentRepository incidents,
                               IncidentService incidentService,
                               RemediationExecutionRepository executions,
                               VerificationRunRepository runs,
                               ObjectMapper mapper,
                               Clock clock,
                               IncidentMetrics metrics,
                               @Value("${irp.verification.max-attempts:2}") int maxAttempts) {
        this.incidents = incidents;
        this.incidentService = incidentService;
        this.executions = executions;
        this.runs = runs;
        this.mapper = mapper;
        this.clock = clock;
        this.metrics = metrics;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public Incident record(UUID incidentId, VerificationRequest request) {
        Incident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        if (incident.getStatus() != IncidentStatus.VERIFYING) {
            throw new IllegalIncidentTransitionException(incident.getStatus(), IncidentStatus.RESOLVED);
        }

        int attempt = incident.incrementVerificationAttempts();
        boolean executionSucceeded = executions.findByIncidentIdOrderByStartedAtDesc(incidentId)
                .stream()
                .anyMatch(execution -> "SUCCEEDED".equals(execution.getStatus()));
        String outcome = VerificationPolicy.effectiveOutcome(request.outcome(), executionSucceeded);
        Instant now = clock.instant();
        runs.save(new VerificationRun(
                UUID.randomUUID(),
                incidentId,
                attempt,
                outcome,
                request.summary(),
                toJson(request.signals()),
                now
        ));
        metrics.recordVerification(outcome);

        if ("RESOLVED".equals(outcome)) {
            return incidentService.resolve(incidentId, "verification-agent", request.summary());
        }
        if (VerificationPolicy.shouldRetry(attempt, maxAttempts)) {
            return incidentService.transition(
                    incidentId,
                    IncidentStatus.INVESTIGATING,
                    "VERIFICATION_RETRY",
                    "verification-agent",
                    "attempt " + attempt + "/" + maxAttempts + ": " + nullToEmpty(request.summary())
            );
        }
        metrics.recordVerificationEscalated();
        incidentService.recordAudit(
                incidentId,
                "VERIFICATION_ESCALATED",
                "verification-agent",
                "retry budget exhausted after " + attempt + " attempts; human resolve required"
        );
        return incident;
    }

    @Transactional(readOnly = true)
    public VerificationView view(UUID incidentId) {
        Incident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        List<VerificationRun> history = runs.findByIncidentIdOrderByCreatedAtDesc(incidentId);
        RunView latest = history.isEmpty() ? null : RunView.from(history.getFirst());
        return new VerificationView(incidentId, incident.getVerificationAttempts(), maxAttempts, latest);
    }

    private String toJson(List<String> signals) {
        try {
            return mapper.writeValueAsString(signals == null ? List.of() : signals);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
