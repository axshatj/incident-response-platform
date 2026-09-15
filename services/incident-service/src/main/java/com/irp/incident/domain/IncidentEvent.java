package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record for every state transition or noteworthy event on an
 * incident. Never update or delete rows in this table.
 */
@Entity
@Table(name = "incident_events")
public class IncidentEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", updatable = false)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", updatable = false)
    private IncidentStatus toStatus;

    @Column(name = "actor", updatable = false)
    private String actor;

    @Column(name = "note", updatable = false)
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected IncidentEvent() {
        // for JPA
    }

    public IncidentEvent(UUID id,
                         UUID incidentId,
                         String eventType,
                         IncidentStatus fromStatus,
                         IncidentStatus toStatus,
                         String actor,
                         String note,
                         Instant occurredAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getEventType() { return eventType; }
    public IncidentStatus getFromStatus() { return fromStatus; }
    public IncidentStatus getToStatus() { return toStatus; }
    public String getActor() { return actor; }
    public String getNote() { return note; }
    public Instant getOccurredAt() { return occurredAt; }
}
