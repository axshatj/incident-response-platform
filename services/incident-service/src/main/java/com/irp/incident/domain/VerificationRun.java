package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_runs")
public class VerificationRun {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "attempt", nullable = false, updatable = false)
    private int attempt;

    @Column(name = "outcome", nullable = false, updatable = false)
    private String outcome;

    @Column(name = "summary", updatable = false)
    private String summary;

    @Column(name = "signals_json", nullable = false, updatable = false)
    private String signalsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationRun() {}

    public VerificationRun(UUID id,
                           UUID incidentId,
                           int attempt,
                           String outcome,
                           String summary,
                           String signalsJson,
                           Instant createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.attempt = attempt;
        this.outcome = outcome;
        this.summary = summary;
        this.signalsJson = signalsJson;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public int getAttempt() { return attempt; }
    public String getOutcome() { return outcome; }
    public String getSummary() { return summary; }
    public String getSignalsJson() { return signalsJson; }
    public Instant getCreatedAt() { return createdAt; }
}
