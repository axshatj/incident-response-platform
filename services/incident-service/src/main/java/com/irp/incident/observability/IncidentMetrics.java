package com.irp.incident.observability;

import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Domain-level metrics for the incident-service. Prefer these narrow, tagged
 * counters over ad-hoc logging - they show up directly on the Grafana
 * dashboard and can be alerted on.
 */
@Component
public class IncidentMetrics {

    // Names use dot notation so Micrometer converts them consistently across
    // registries. We intentionally avoid a "_created" segment because
    // Prometheus/OpenMetrics reserves it for auto-generated counter timestamps.
    private static final String CREATED = "irp.incident.opened";
    private static final String TRANSITIONS = "irp.incident.transitions";
    private static final String ILLEGAL = "irp.incident.illegal_transitions";

    // Phase 3 - eventing
    private static final String ALERTS_CONSUMED = "irp.alerts.consumed";
    private static final String EVENTS_PUBLISHED = "irp.events.published";
    private static final String EVENTS_PUBLISH_FAILED = "irp.events.publish_failed";
    private static final String RAG_RETRIEVED = "irp.rag.retrieved";

    private final MeterRegistry registry;

    // Cache built counters to avoid re-resolving on every increment.
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public IncidentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCreated(Severity severity, String environment) {
        counter(CREATED,
                "severity", severity.name(),
                "environment", environment)
                .increment();
    }

    public void recordTransition(IncidentStatus from, IncidentStatus to) {
        counter(TRANSITIONS,
                "from", from == null ? "NONE" : from.name(),
                "to", to.name())
                .increment();
    }

    public void recordIllegalTransition(IncidentStatus from, IncidentStatus to) {
        counter(ILLEGAL,
                "from", from.name(),
                "to", to.name())
                .increment();
    }

    public void recordAlertConsumed(String result) {
        counter(ALERTS_CONSUMED, "result", result).increment();
    }

    public void recordEventPublished(String eventType) {
        counter(EVENTS_PUBLISHED, "event_type", eventType).increment();
    }

    public void recordEventPublishFailed(String eventType) {
        counter(EVENTS_PUBLISH_FAILED, "event_type", eventType).increment();
    }

    public void recordRagRetrieved(int documentCount) {
        counter(RAG_RETRIEVED).increment(documentCount);
    }

    private Counter counter(String name, String... tags) {
        StringBuilder key = new StringBuilder(name);
        for (String tag : tags) {
            key.append('|').append(tag);
        }
        return counters.computeIfAbsent(key.toString(),
                k -> Counter.builder(name).tags(tags).register(registry));
    }
}
