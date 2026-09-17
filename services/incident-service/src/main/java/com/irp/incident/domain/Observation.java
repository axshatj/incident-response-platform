package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "observations")
public class Observation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "investigation_id", updatable = false)
    private UUID investigationId;

    @Column(name = "type", nullable = false, updatable = false)
    private String type;

    @Column(name = "source", nullable = false, updatable = false)
    private String source;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private Instant observedAt;

    @Column(name = "content", nullable = false, updatable = false)
    private String content;

    @Column(name = "confidence", updatable = false)
    private Double confidence;

    @Column(name = "trace_id", updatable = false)
    private String traceId;

    protected Observation() {}

    public Observation(UUID id,
                       UUID incidentId,
                       UUID investigationId,
                       String type,
                       String source,
                       Instant observedAt,
                       String content,
                       Double confidence,
                       String traceId) {
        this.id = id;
        this.incidentId = incidentId;
        this.investigationId = investigationId;
        this.type = type;
        this.source = source;
        this.observedAt = observedAt;
        this.content = content;
        this.confidence = confidence;
        this.traceId = traceId;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getInvestigationId() { return investigationId; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public Instant getObservedAt() { return observedAt; }
    public String getContent() { return content; }
    public Double getConfidence() { return confidence; }
    public String getTraceId() { return traceId; }
}
