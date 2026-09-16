package com.irp.incident.eventing;

import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.observability.IncidentMetrics;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publishes lifecycle events to Kafka. Failures are logged and counted but do
 * NOT roll back the surrounding transaction - the DB is authoritative and the
 * outbox pattern is deferred to a later hardening phase (see SKILL.md).
 */
@Component
public class IncidentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);

    private final KafkaTemplate<String, Object> template;
    private final IncidentMetrics metrics;

    public IncidentEventPublisher(KafkaTemplate<String, Object> template,
                                  IncidentMetrics metrics) {
        this.template = template;
        this.metrics = metrics;
    }

    public void publishDetected(Incident incident) {
        IncidentDetectedPayload payload = new IncidentDetectedPayload(
                incident.getId(),
                incident.getExternalId(),
                incident.getService(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getEnvironment(),
                incident.getDetectedAt()
        );
        EventEnvelope<IncidentDetectedPayload> envelope = EventEnvelope.newEnvelope(
                EventTypes.INCIDENT_DETECTED,
                1,
                incident.getExternalId() != null ? incident.getExternalId() : incident.getId().toString(),
                EventTypes.SOURCE_INCIDENT_SERVICE,
                payload
        );
        send(KafkaTopics.INCIDENT_DETECTED, envelope);
    }

    public void publishUpdated(Incident incident,
                               IncidentStatus from,
                               IncidentStatus to,
                               String eventType,
                               String actor,
                               java.time.Instant occurredAt) {
        IncidentUpdatedPayload payload = new IncidentUpdatedPayload(
                incident.getId(),
                incident.getExternalId(),
                from,
                to,
                eventType,
                actor,
                occurredAt
        );
        EventEnvelope<IncidentUpdatedPayload> envelope = EventEnvelope.newEnvelope(
                EventTypes.INCIDENT_UPDATED,
                1,
                incident.getExternalId() != null ? incident.getExternalId() : incident.getId().toString(),
                EventTypes.SOURCE_INCIDENT_SERVICE,
                payload
        );
        send(KafkaTopics.INCIDENT_UPDATED, envelope);
    }

    private <T> void send(String topic, EventEnvelope<T> envelope) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topic,
                null,
                envelope.correlationId(), // key: preserves ordering per correlation
                envelope
        );
        record.headers().add(new RecordHeader("event-type", envelope.eventType().getBytes()));
        record.headers().add(new RecordHeader("event-id", envelope.eventId().toString().getBytes()));
        record.headers().add(new RecordHeader("schema-version",
                Integer.toString(envelope.schemaVersion()).getBytes()));
        record.headers().add(new RecordHeader("source", envelope.source().getBytes()));

        CompletableFuture<SendResult<String, Object>> future = template.send(record);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                metrics.recordEventPublishFailed(envelope.eventType());
                log.warn("Failed to publish {} to {}: {}", envelope.eventType(), topic, ex.getMessage());
            } else {
                metrics.recordEventPublished(envelope.eventType());
            }
        });
    }
}
