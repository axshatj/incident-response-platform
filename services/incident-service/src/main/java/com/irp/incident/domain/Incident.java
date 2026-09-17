package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Incident aggregate. State transitions must go through
 * {@link #transitionTo(IncidentStatus, Instant)} which enforces the state
 * machine defined in {@link IncidentStatus}.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "service", nullable = false)
    private String service;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status;

    @Column(name = "environment", nullable = false)
    private String environment;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "root_cause_id")
    private UUID rootCauseId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Incident() {
        // for JPA
    }

    public Incident(UUID id,
                    String externalId,
                    String service,
                    String title,
                    String description,
                    Severity severity,
                    String environment,
                    Instant detectedAt) {
        this.id = id;
        this.externalId = externalId;
        this.service = service;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.environment = environment;
        this.detectedAt = detectedAt;
        this.status = IncidentStatus.DETECTED;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getService() { return service; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public String getEnvironment() { return environment; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public UUID getRootCauseId() { return rootCauseId; }
    public Long getVersion() { return version; }

    public void attachRootCause(UUID rootCauseId) {
        this.rootCauseId = rootCauseId;
    }

    /**
     * Attempts to move this incident into {@code target}. Throws
     * {@link IllegalIncidentTransitionException} if the transition is not
     * permitted by {@link IncidentStatus#ALLOWED_TRANSITIONS}.
     *
     * @return the previous status (useful for building audit events)
     */
    public IncidentStatus transitionTo(IncidentStatus target, Instant at) {
        IncidentStatus previous = this.status;
        if (!previous.canTransitionTo(target)) {
            throw new IllegalIncidentTransitionException(previous, target);
        }
        this.status = target;
        if (target.isTerminal()) {
            this.resolvedAt = at;
        }
        return previous;
    }
}
