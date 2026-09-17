package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "root_causes")
public class RootCause {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "statement", nullable = false, updatable = false)
    private String statement;

    @Column(name = "confidence", nullable = false, updatable = false)
    private double confidence;

    @Column(name = "evidence_json", nullable = false, updatable = false)
    private String evidenceJson;

    @Column(name = "counter_evidence_json", nullable = false, updatable = false)
    private String counterEvidenceJson;

    @Column(name = "affected_json", nullable = false, updatable = false)
    private String affectedJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RootCause() {}

    public RootCause(UUID id,
                     UUID incidentId,
                     String statement,
                     double confidence,
                     String evidenceJson,
                     String counterEvidenceJson,
                     String affectedJson,
                     Instant createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.statement = statement;
        this.confidence = confidence;
        this.evidenceJson = evidenceJson;
        this.counterEvidenceJson = counterEvidenceJson;
        this.affectedJson = affectedJson;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getStatement() { return statement; }
    public double getConfidence() { return confidence; }
    public String getEvidenceJson() { return evidenceJson; }
    public String getCounterEvidenceJson() { return counterEvidenceJson; }
    public String getAffectedJson() { return affectedJson; }
    public Instant getCreatedAt() { return createdAt; }
}
