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

    private static final String CREATED = "irp_incidents_created_total";
    private static final String TRANSITIONS = "irp_incident_transitions_total";
    private static final String ILLEGAL = "irp_incident_illegal_transitions_total";

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

    private Counter counter(String name, String... tags) {
        StringBuilder key = new StringBuilder(name);
        for (String tag : tags) {
            key.append('|').append(tag);
        }
        return counters.computeIfAbsent(key.toString(),
                k -> Counter.builder(name).tags(tags).register(registry));
    }
}
