package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "remediation_plans")
public class RemediationPlan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "namespace", nullable = false, updatable = false)
    private String namespace;

    @Column(name = "deployment", nullable = false, updatable = false)
    private String deployment;

    @Column(name = "target_revision", nullable = false, updatable = false)
    private int targetRevision;

    @Column(name = "risk", nullable = false, updatable = false)
    private String risk;

    @Column(name = "decision", nullable = false, updatable = false)
    private String decision;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "expected_impact", updatable = false)
    private String expectedImpact;

    @Column(name = "blast_radius", updatable = false)
    private String blastRadius;

    @Column(name = "confidence", updatable = false)
    private Double confidence;

    @Column(name = "rationale", updatable = false)
    private String rationale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RemediationPlan() {}

    public RemediationPlan(UUID id,
                           UUID incidentId,
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
                           Instant createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.action = action;
        this.namespace = namespace;
        this.deployment = deployment;
        this.targetRevision = targetRevision;
        this.risk = risk;
        this.decision = decision;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.expectedImpact = expectedImpact;
        this.blastRadius = blastRadius;
        this.confidence = confidence;
        this.rationale = rationale;
        this.createdAt = createdAt;
    }

    public void markStatus(String status) {
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getAction() { return action; }
    public String getNamespace() { return namespace; }
    public String getDeployment() { return deployment; }
    public int getTargetRevision() { return targetRevision; }
    public String getRisk() { return risk; }
    public String getDecision() { return decision; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getExpectedImpact() { return expectedImpact; }
    public String getBlastRadius() { return blastRadius; }
    public Double getConfidence() { return confidence; }
    public String getRationale() { return rationale; }
    public Instant getCreatedAt() { return createdAt; }
}
