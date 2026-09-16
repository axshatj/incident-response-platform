package com.irp.incident.eventing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.incident.domain.Incident;
import com.irp.incident.observability.IncidentMetrics;
import com.irp.incident.service.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code telemetry.alerts} and turns each valid alert into an
 * incident. Idempotency is guaranteed by the unique constraint on
 * {@code incidents.external_id}: a redelivered alert with the same
 * {@code alertId} results in a {@link DataIntegrityViolationException} that
 * we swallow and count as a duplicate.
 *
 * Invalid payloads (missing required fields, unparseable JSON) are surfaced by
 * throwing, which the DefaultErrorHandler retries then routes to the DLT.
 */
@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final ObjectMapper mapper;
    private final IncidentService incidents;
    private final IncidentMetrics metrics;

    public AlertConsumer(ObjectMapper mapper,
                         IncidentService incidents,
                         IncidentMetrics metrics) {
        this.mapper = mapper;
        this.incidents = incidents;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = KafkaTopics.TELEMETRY_ALERTS,
            groupId = "${spring.kafka.consumer.group-id:incident-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAlert(String rawValue) {
        EventEnvelope<AlertPayload> envelope = parse(rawValue);
        AlertPayload alert = envelope.payload();
        validate(envelope, alert);

        try {
            Incident created = incidents.createFromAlert(alert);
            metrics.recordAlertConsumed("created");
            log.info("Created incident {} from alert {} (correlationId={})",
                    created.getId(), alert.alertId(), envelope.correlationId());
        } catch (DataIntegrityViolationException dup) {
            metrics.recordAlertConsumed("duplicate");
            log.info("Duplicate alert {} ignored (correlationId={})",
                    alert.alertId(), envelope.correlationId());
        }
    }

    private EventEnvelope<AlertPayload> parse(String raw) {
        try {
            return mapper.readValue(raw, new TypeReference<EventEnvelope<AlertPayload>>() {});
        } catch (Exception e) {
            metrics.recordAlertConsumed("invalid");
            throw new IllegalArgumentException("Unparseable alert envelope: " + e.getMessage(), e);
        }
    }

    private void validate(EventEnvelope<AlertPayload> envelope, AlertPayload alert) {
        if (!EventTypes.TELEMETRY_ALERT.equals(envelope.eventType())) {
            metrics.recordAlertConsumed("invalid");
            throw new IllegalArgumentException("Unexpected eventType: " + envelope.eventType());
        }
        if (alert == null || alert.alertId() == null || alert.alertId().isBlank()) {
            metrics.recordAlertConsumed("invalid");
            throw new IllegalArgumentException("Alert payload missing required alertId");
        }
        if (alert.service() == null || alert.title() == null
                || alert.severity() == null || alert.environment() == null) {
            metrics.recordAlertConsumed("invalid");
            throw new IllegalArgumentException(
                    "Alert payload missing required fields (service/title/severity/environment)");
        }
    }
}
