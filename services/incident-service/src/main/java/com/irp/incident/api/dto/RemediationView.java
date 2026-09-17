package com.irp.incident.api.dto;

import com.irp.incident.domain.RemediationExecution;
import com.irp.incident.domain.RemediationPlan;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RemediationView(
        UUID incidentId,
        PlanView plan,
        List<ExecutionView> executions
) {
    public record PlanView(
            UUID id,
            String action,
            String namespace,
            String deployment,
            int targetRevision,
            String risk,
            String decision,
            String status,
            String idempotencyKey,
            String expectedImpact,
            String blastRadius,
            Double confidence,
            String rationale,
            Instant createdAt
    ) {
        public static PlanView from(RemediationPlan plan) {
            return new PlanView(
                    plan.getId(),
                    plan.getAction(),
                    plan.getNamespace(),
                    plan.getDeployment(),
                    plan.getTargetRevision(),
                    plan.getRisk(),
                    plan.getDecision(),
                    plan.getStatus(),
                    plan.getIdempotencyKey(),
                    plan.getExpectedImpact(),
                    plan.getBlastRadius(),
                    plan.getConfidence(),
                    plan.getRationale(),
                    plan.getCreatedAt()
            );
        }
    }

    public record ExecutionView(
            UUID id,
            UUID planId,
            String idempotencyKey,
            String status,
            String resultPreview,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {
        public static ExecutionView from(RemediationExecution execution) {
            return new ExecutionView(
                    execution.getId(),
                    execution.getPlanId(),
                    execution.getIdempotencyKey(),
                    execution.getStatus(),
                    execution.getResultPreview(),
                    execution.getErrorMessage(),
                    execution.getStartedAt(),
                    execution.getCompletedAt()
            );
        }
    }
}
