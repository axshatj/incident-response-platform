package com.irp.incident.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.irp.incident.domain.Severity;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void alertEnvelopeRoundTrip() throws Exception {
        AlertPayload alert = new AlertPayload(
                "am-123",
                "payment-service",
                "DB pool exhausted",
                "p99 > 2s",
                Severity.SEV2,
                "prod",
                Map.of("region", "us-east-1")
        );
        EventEnvelope<AlertPayload> envelope = EventEnvelope.newEnvelope(
                EventTypes.TELEMETRY_ALERT, 1, "am-123", "alertmanager", alert);

        String json = mapper.writeValueAsString(envelope);
        EventEnvelope<AlertPayload> parsed = mapper.readValue(
                json, new TypeReference<EventEnvelope<AlertPayload>>() {});

        assertThat(parsed.eventId()).isEqualTo(envelope.eventId());
        assertThat(parsed.eventType()).isEqualTo(EventTypes.TELEMETRY_ALERT);
        assertThat(parsed.schemaVersion()).isEqualTo(1);
        assertThat(parsed.correlationId()).isEqualTo("am-123");
        assertThat(parsed.source()).isEqualTo("alertmanager");
        assertThat(parsed.payload().alertId()).isEqualTo("am-123");
        assertThat(parsed.payload().severity()).isEqualTo(Severity.SEV2);
        assertThat(parsed.payload().labels()).containsEntry("region", "us-east-1");
    }

    @Test
    void factoryPopulatesFields() {
        EventEnvelope<String> e = EventEnvelope.newEnvelope("t", 2, "c", "s", "hi");
        assertThat(e.eventId()).isNotNull();
        assertThat(e.timestamp()).isNotNull();
        assertThat(e.eventType()).isEqualTo("t");
        assertThat(e.schemaVersion()).isEqualTo(2);
        assertThat(e.correlationId()).isEqualTo("c");
        assertThat(e.source()).isEqualTo("s");
        assertThat(e.payload()).isEqualTo("hi");
    }
}
