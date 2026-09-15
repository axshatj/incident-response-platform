package com.irp.incident.service;

import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentEvent;
import com.irp.incident.domain.IncidentNotFoundException;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.Severity;
import com.irp.incident.repository.IncidentEventRepository;
import com.irp.incident.repository.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the incident lifecycle. Every mutation:
 *   1. loads the aggregate,
 *   2. delegates the transition to {@link Incident} (state-machine validated),
 *   3. writes an immutable {@link IncidentEvent} audit record.
 *
 * Steps 2 and 3 happen inside one transaction so state and audit trail are
 * always consistent.
 */
@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final IncidentEventRepository events;
    private final Clock clock;

    public IncidentService(IncidentRepository incidents,
                           IncidentEventRepository events,
                           Clock clock) {
        this.incidents = incidents;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Incident create(String externalId,
                           String service,
                           String title,
                           String description,
                           Severity severity,
                           String environment) {
        Instant now = clock.instant();
        Incident incident = new Incident(
                UUID.randomUUID(),
                externalId,
                service,
                title,
                description,
                severity,
                environment,
                now
        );
        Incident saved = incidents.save(incident);
        writeEvent(saved.getId(), "INCIDENT_CREATED", null, IncidentStatus.DETECTED,
                "system", "Incident detected", now);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Incident> listAll() {
        return incidents.findAllByOrderByDetectedAtDesc();
    }

    @Transactional(readOnly = true)
    public Incident get(UUID id) {
        return incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<IncidentEvent> timeline(UUID incidentId) {
        get(incidentId); // enforces 404 semantics
        return events.findByIncidentIdOrderByOccurredAtAsc(incidentId);
    }

    /** DETECTED -> TRIAGING */
    @Transactional
    public Incident acknowledge(UUID id, String actor, String note) {
        return transition(id, IncidentStatus.TRIAGING, "INCIDENT_ACKNOWLEDGED", actor, note);
    }

    /** AWAITING_APPROVAL -> REMEDIATING */
    @Transactional
    public Incident approve(UUID id, String actor, String note) {
        return transition(id, IncidentStatus.REMEDIATING, "REMEDIATION_APPROVED", actor, note);
    }

    /** AWAITING_APPROVAL -> INVESTIGATING */
    @Transactional
    public Incident reject(UUID id, String actor, String note) {
        return transition(id, IncidentStatus.INVESTIGATING, "REMEDIATION_REJECTED", actor, note);
    }

    /** VERIFYING -> RESOLVED */
    @Transactional
    public Incident resolve(UUID id, String actor, String note) {
        return transition(id, IncidentStatus.RESOLVED, "INCIDENT_RESOLVED", actor, note);
    }

    /**
     * Generic transition. Intended for internal callers (agents, workers) and
     * dev/admin flows. The transition MUST be legal per the state machine.
     */
    @Transactional
    public Incident transition(UUID id,
                               IncidentStatus target,
                               String eventType,
                               String actor,
                               String note) {
        Incident incident = incidents.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
        Instant now = clock.instant();
        IncidentStatus previous = incident.transitionTo(target, now);
        writeEvent(incident.getId(), eventType, previous, target, actor, note, now);
        return incident;
    }

    private void writeEvent(UUID incidentId,
                            String eventType,
                            IncidentStatus from,
                            IncidentStatus to,
                            String actor,
                            String note,
                            Instant occurredAt) {
        events.save(new IncidentEvent(
                UUID.randomUUID(),
                incidentId,
                eventType,
                from,
                to,
                actor,
                note,
                occurredAt
        ));
    }
}
