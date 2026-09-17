package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remediation_executions")
public class RemediationExecution {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "plan_id", updatable = false)
    private UUID planId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "result_preview", updatable = false)
    private String resultPreview;

    @Column(name = "error_message", updatable = false)
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at", updatable = false)
    private Instant completedAt;

    protected RemediationExecution() {}

    public RemediationExecution(UUID id,
                                UUID incidentId,
                                UUID planId,
                                String idempotencyKey,
                                String status,
                                String resultPreview,
                                String errorMessage,
                                Instant startedAt,
                                Instant completedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.planId = planId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.resultPreview = resultPreview;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getPlanId() { return planId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public String getResultPreview() { return resultPreview; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
