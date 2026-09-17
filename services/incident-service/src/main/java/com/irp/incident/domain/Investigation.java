package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigations")
public class Investigation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "summary")
    private String summary;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Investigation() {}

    public Investigation(UUID id, UUID incidentId, String status, Instant startedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.status = status;
        this.startedAt = startedAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getStatus() { return status; }
    public String getSummary() { return summary; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void complete(String summary, Instant at) {
        this.status = "COMPLETED";
        this.summary = summary;
        this.completedAt = at;
    }
}
