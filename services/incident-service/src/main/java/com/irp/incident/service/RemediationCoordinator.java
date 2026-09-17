package com.irp.incident.service;

import com.irp.incident.api.dto.RemediationExecutionRequest;
import com.irp.incident.api.dto.RemediationPlanRequest;
import com.irp.incident.api.dto.RemediationView;
import com.irp.incident.api.dto.RemediationView.ExecutionView;
import com.irp.incident.api.dto.RemediationView.PlanView;
import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.PolicyRejectedException;
import com.irp.incident.domain.RemediationExecution;
import com.irp.incident.domain.RemediationPlan;
import com.irp.incident.observability.IncidentMetrics;
import com.irp.incident.policy.PolicyDecisionType;
import com.irp.incident.policy.PolicyEngine;
import com.irp.incident.policy.PolicyVerdict;
import com.irp.incident.repository.RemediationExecutionRepository;
import com.irp.incident.repository.RemediationPlanRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemediationCoordinator {

    private final IncidentService incidents;
    private final PolicyEngine policy;
    private final RemediationPlanRepository plans;
    private final RemediationExecutionRepository executions;
    private final Clock clock;
    private final IncidentMetrics metrics;

    public RemediationCoordinator(IncidentService incidents,
                                  PolicyEngine policy,
                                  RemediationPlanRepository plans,
                                  RemediationExecutionRepository executions,
                                  Clock clock,
                                  IncidentMetrics metrics) {
        this.incidents = incidents;
        this.policy = policy;
        this.plans = plans;
        this.executions = executions;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public RemediationView propose(UUID incidentId, RemediationPlanRequest request) {
        Incident incident = incidents.get(incidentId);
        String action = PolicyEngine.normalize(request.action());
        PolicyVerdict verdict = policy.evaluate(action, incident.getEnvironment(), request.namespace());
        if (verdict.decision() == PolicyDecisionType.PROHIBITED) {
            metrics.recordRemediationProhibited(action);
            throw new PolicyRejectedException(action, verdict.risk().name(), verdict.reason());
        }

        String key = idempotencyKey(incidentId, action, request.namespace(), request.deployment(), request.targetRevision());
        RemediationPlan existing = plans.findByIdempotencyKey(key).orElse(null);
        if (existing != null) {
            return view(incidentId);
        }

        String status = verdict.decision() == PolicyDecisionType.REQUIRE_APPROVAL
                ? "AWAITING_APPROVAL"
                : "APPROVED";
        RemediationPlan plan = new RemediationPlan(
                UUID.randomUUID(),
                incidentId,
                action,
                request.namespace(),
                request.deployment(),
                request.targetRevision(),
                verdict.risk().name(),
                verdict.decision().name(),
                status,
                key,
                request.expectedImpact(),
                request.blastRadius(),
                request.confidence(),
                request.rationale(),
                clock.instant()
        );
        plans.save(plan);
        metrics.recordRemediationProposed(verdict.risk().name());

        incidents.transition(incidentId, IncidentStatus.REMEDIATION_PROPOSED,
                "REMEDIATION_PROPOSED", "remediation-planner", action + " risk=" + verdict.risk());
        if (verdict.decision() == PolicyDecisionType.REQUIRE_APPROVAL) {
            incidents.transition(incidentId, IncidentStatus.AWAITING_APPROVAL,
                    "APPROVAL_REQUIRED", "policy-engine", verdict.reason());
        } else {
            incidents.transition(incidentId, IncidentStatus.REMEDIATING,
                    "REMEDIATION_AUTO_APPROVED", "policy-engine", verdict.reason());
        }
        return view(incidentId);
    }

    @Transactional
    public Incident approve(UUID incidentId, String actor, String note) {
        latestPlan(incidentId).ifPresent(plan -> plan.markStatus("APPROVED"));
        return incidents.approve(incidentId, actor, note);
    }

    @Transactional
    public Incident reject(UUID incidentId, String actor, String note) {
        latestPlan(incidentId).ifPresent(plan -> plan.markStatus("REJECTED"));
        return incidents.reject(incidentId, actor, note);
    }

    @Transactional
    public RemediationView recordExecution(UUID incidentId, RemediationExecutionRequest request) {
        incidents.get(incidentId);
        RemediationExecution existing = executions.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing == null) {
            Instant started = request.startedAt() == null ? clock.instant() : request.startedAt();
            Instant completed = request.completedAt() == null ? clock.instant() : request.completedAt();
            executions.save(new RemediationExecution(
                    UUID.randomUUID(),
                    incidentId,
                    request.planId(),
                    request.idempotencyKey(),
                    request.status(),
                    request.resultPreview(),
                    request.errorMessage(),
                    started,
                    completed
            ));
            if ("SUCCEEDED".equals(request.status())) {
                latestPlan(incidentId).ifPresent(plan -> plan.markStatus("EXECUTED"));
                metrics.recordRemediationExecuted("succeeded");
            } else {
                latestPlan(incidentId).ifPresent(plan -> plan.markStatus("FAILED"));
                metrics.recordRemediationExecuted("failed");
            }
        }
        Incident incident = incidents.get(incidentId);
        if ("SUCCEEDED".equals(request.status()) && incident.getStatus() == IncidentStatus.REMEDIATING) {
            incidents.transition(incidentId, IncidentStatus.VERIFYING,
                    "REMEDIATION_EXECUTED", "remediation-service", request.resultPreview());
        }
        return view(incidentId);
    }

    @Transactional(readOnly = true)
    public RemediationView view(UUID incidentId) {
        incidents.get(incidentId);
        PlanView plan = latestPlan(incidentId).map(PlanView::from).orElse(null);
        List<ExecutionView> execs = executions.findByIncidentIdOrderByStartedAtDesc(incidentId)
                .stream().map(ExecutionView::from).toList();
        return new RemediationView(incidentId, plan, execs);
    }

    private java.util.Optional<RemediationPlan> latestPlan(UUID incidentId) {
        return plans.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream().findFirst();
    }

    static String idempotencyKey(UUID incidentId, String action, String namespace, String deployment, int revision) {
        return incidentId + ":" + action + ":" + namespace + ":" + deployment + ":" + revision;
    }
}
