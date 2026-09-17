package com.irp.incident.service;

import com.irp.incident.domain.IllegalIncidentTransitionException;
import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentEvent;
import com.irp.incident.domain.IncidentNotFoundException;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.Severity;
import com.irp.incident.eventing.AlertPayload;
import com.irp.incident.eventing.IncidentEventPublisher;
import com.irp.incident.observability.IncidentMetrics;
import com.irp.incident.repository.IncidentEventRepository;
import com.irp.incident.repository.IncidentRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidents;
    private final IncidentEventRepository events;
    private final Clock clock;
    private final IncidentMetrics metrics;
    private final ObservationRegistry observations;
    private final IncidentEventPublisher eventPublisher;

    public IncidentService(IncidentRepository incidents,
                           IncidentEventRepository events,
                           Clock clock,
                           IncidentMetrics metrics,
                           ObservationRegistry observations,
                           IncidentEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.events = events;
        this.clock = clock;
        this.metrics = metrics;
        this.observations = observations;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Incident create(String externalId,
                           String service,
                           String title,
                           String description,
                           Severity severity,
                           String environment) {
        return Observation.createNotStarted("incident.create", observations)
                .lowCardinalityKeyValue("severity", severity.name())
                .lowCardinalityKeyValue("environment", environment)
                .observe(() -> {
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
                    metrics.recordCreated(severity, environment);
                    metrics.recordTransition(null, IncidentStatus.DETECTED);
                    log.info("Incident created id={} service={} severity={} environment={}",
                            saved.getId(), service, severity, environment);
                    // Best-effort publish. On broker outage the event is dropped
                    // and counted; the DB remains authoritative. See SKILL.md
                    // for the outbox-pattern follow-up.
                    eventPublisher.publishDetected(saved);
                    return saved;
                });
    }

    /**
     * Convenience overload used by the alert consumer. The {@code alertId} is
     * stored as the incident's {@code external_id}, giving us idempotency at
     * the DB layer: a duplicate alert throws
     * {@link org.springframework.dao.DataIntegrityViolationException} which
     * the consumer treats as a benign duplicate.
     */
    @Transactional
    public Incident createFromAlert(AlertPayload alert) {
        return create(
                alert.alertId(),
                alert.service(),
                alert.title(),
                alert.description(),
                alert.severity(),
                alert.environment()
        );
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
     * Audit-only write that does not change status. Used when verification
     * exhausts its retry budget and a human must resolve from VERIFYING.
     */
    @Transactional
    public Incident recordAudit(UUID id, String eventType, String actor, String note) {
        Incident incident = incidents.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
        Instant now = clock.instant();
        writeEvent(incident.getId(), eventType, incident.getStatus(), incident.getStatus(), actor, note, now);
        eventPublisher.publishUpdated(incident, incident.getStatus(), incident.getStatus(), eventType, actor, now);
        return incident;
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
        return Observation.createNotStarted("incident.transition", observations)
                .lowCardinalityKeyValue("target", target.name())
                .lowCardinalityKeyValue("eventType", eventType)
                .observe(() -> {
                    Incident incident = incidents.findById(id)
                            .orElseThrow(() -> new IncidentNotFoundException(id));
                    Instant now = clock.instant();
                    IncidentStatus previous;
                    try {
                        previous = incident.transitionTo(target, now);
                    } catch (IllegalIncidentTransitionException ex) {
                        metrics.recordIllegalTransition(ex.getFrom(), ex.getTo());
                        log.warn("Illegal transition rejected id={} {} -> {} actor={}",
                                id, ex.getFrom(), ex.getTo(), actor);
                        throw ex;
                    }
                    writeEvent(incident.getId(), eventType, previous, target, actor, note, now);
                    metrics.recordTransition(previous, target);
                    log.info("Incident transitioned id={} {} -> {} eventType={} actor={}",
                            incident.getId(), previous, target, eventType, actor);
                    eventPublisher.publishUpdated(incident, previous, target, eventType, actor, now);
                    return incident;
                });
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
