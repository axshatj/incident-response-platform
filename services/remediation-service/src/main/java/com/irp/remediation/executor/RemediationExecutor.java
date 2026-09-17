package com.irp.remediation.executor;

import com.irp.remediation.client.IncidentServiceClient;
import com.irp.remediation.client.IncidentServiceClient.ExecutionRequest;
import com.irp.remediation.client.IncidentServiceClient.PlanView;
import com.irp.remediation.k8s.SimulatedKubernetesClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Deterministic executor. Re-validates allowlists and never trusts the LLM.
 * HIGH-risk actions only run after the incident is already in REMEDIATING
 * (human approval or LOW auto-approve already happened in incident-service).
 */
@Service
public class RemediationExecutor {

    private static final Logger log = LoggerFactory.getLogger(RemediationExecutor.class);
    private static final Set<String> CRITICAL = Set.of(
            "SHELL", "KUBECTL", "DROP_DATABASE", "DELETE_DATABASE", "IAM_CHANGE");

    private final IncidentServiceClient incidents;
    private final SimulatedKubernetesClient kubernetes;
    private final MeterRegistry registry;

    public RemediationExecutor(IncidentServiceClient incidents,
                               SimulatedKubernetesClient kubernetes,
                               MeterRegistry registry) {
        this.incidents = incidents;
        this.kubernetes = kubernetes;
        this.registry = registry;
    }

    public void executeIfRemediating(UUID incidentId) {
        var incident = incidents.get(incidentId);
        if (incident == null || !"REMEDIATING".equals(incident.status())) {
            return;
        }
        var view = incidents.remediation(incidentId);
        if (view == null || view.plan() == null) {
            log.warn("No remediation plan for {} — skipping execute", incidentId);
            return;
        }
        PlanView plan = view.plan();
        Instant started = Instant.now();
        try {
            assertSafe(plan);
            String preview = invoke(plan);
            record(incidentId, plan, "SUCCEEDED", preview, null, started);
            increment("succeeded", plan.action());
        } catch (RuntimeException e) {
            log.warn("Remediation failed for {}: {}", incidentId, e.getMessage());
            record(incidentId, plan, "FAILED", null, e.getMessage(), started);
            increment("failed", plan.action());
        }
    }

    private void assertSafe(PlanView plan) {
        String action = plan.action() == null ? "" : plan.action().toUpperCase(Locale.ROOT);
        if (CRITICAL.contains(action) || action.contains("SHELL") || action.contains("KUBECTL")) {
            throw new IllegalArgumentException("CRITICAL actions are prohibited: " + action);
        }
        if ("HIGH".equals(plan.risk()) && !"APPROVED".equals(plan.status()) && !"EXECUTED".equals(plan.status())) {
            throw new IllegalArgumentException("HIGH-risk plan is not approved");
        }
    }

    private String invoke(PlanView plan) {
        String action = plan.action().toUpperCase(Locale.ROOT);
        return switch (action) {
            case "ROLLBACK_DEPLOYMENT" -> kubernetes.rollback(
                    plan.namespace(), plan.deployment(), plan.targetRevision());
            case "RESTART_POD", "RESTART_CONSUMER" -> kubernetes.restartPod(
                    plan.namespace(), plan.deployment());
            default -> throw new IllegalArgumentException("Executor does not implement " + action);
        };
    }

    private void record(UUID incidentId, PlanView plan, String status, String preview, String error, Instant started) {
        incidents.recordExecution(incidentId, new ExecutionRequest(
                plan.id(),
                plan.idempotencyKey(),
                status,
                preview,
                error,
                started,
                Instant.now()
        ));
    }

    private void increment(String result, String action) {
        Counter.builder("irp.remediation.executor")
                .tag("result", result)
                .tag("action", action == null ? "unknown" : action)
                .register(registry)
                .increment();
    }
}
