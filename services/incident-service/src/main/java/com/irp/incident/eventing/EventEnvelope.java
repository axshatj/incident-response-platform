package com.irp.incident.eventing;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Canonical event envelope for every Kafka message on the platform. Structure
 * mirrors the envelope defined in SKILL.md so consumers written in other
 * services and languages can parse it uniformly.
 *
 * @param <T> concrete payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant timestamp,
        String correlationId,
        String source,
        T payload
) {

    public static <T> EventEnvelope<T> newEnvelope(String eventType,
                                                   int schemaVersion,
                                                   String correlationId,
                                                   String source,
                                                   T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                schemaVersion,
                Instant.now(),
                correlationId,
                source,
                payload
        );
    }
}
