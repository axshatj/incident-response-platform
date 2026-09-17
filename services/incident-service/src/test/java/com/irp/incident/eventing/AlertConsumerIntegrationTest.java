package com.irp.incident.eventing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.irp.incident.domain.Severity;
import com.irp.incident.repository.IncidentRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end proof of the Phase 3 eventing pipeline. Uses Testcontainers to
 * spin up Postgres and Kafka, boots the full application context, publishes a
 * telemetry.alert envelope, and asserts:
 *   1. an incident row is persisted with the alertId as external_id,
 *   2. a corresponding incident.detected envelope is published downstream,
 *   3. redelivery of the same alertId is silently deduped.
 */
@Testcontainers
@SpringBootTest
class AlertConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // Isolate this test run's consumer group.
        registry.add("spring.kafka.consumer.group-id",
                () -> "incident-service-it-" + UUID.randomUUID());
    }

    @Autowired KafkaTemplate<String, Object> template;
    @Autowired IncidentRepository incidents;

    @Test
    void alertBecomesIncidentAndEmitsDownstreamEvent() throws Exception {
        String alertId = "am-" + UUID.randomUUID();
        AlertPayload alert = new AlertPayload(
                alertId,
                "payment-service",
                "DB pool exhausted",
                "p99 > 2s after v42",
                Severity.SEV2,
                "prod",
                Map.of("region", "us-east-1")
        );
        EventEnvelope<AlertPayload> envelope = EventEnvelope.newEnvelope(
                EventTypes.TELEMETRY_ALERT, 1, alertId, "alertmanager", alert);

        // Downstream consumer to verify incident.detected is published.
        try (KafkaConsumer<String, String> downstream = downstreamConsumer("dl-" + alertId)) {
            downstream.subscribe(java.util.List.of(KafkaTopics.INCIDENT_DETECTED));

            // Send the envelope object directly; JsonSerializer on the producer
            // handles the encoding. Passing a pre-serialized String here would
            // double-encode the payload.
            template.send(KafkaTopics.TELEMETRY_ALERTS, alertId, envelope).get();

            // 1. Incident row created.
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                    assertThat(incidents.findAllByOrderByDetectedAtDesc())
                            .anyMatch(i -> alertId.equals(i.getExternalId()))
            );

            // 2. incident.detected published.
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var records = downstream.poll(Duration.ofSeconds(1));
                boolean found = false;
                for (ConsumerRecord<String, String> r : records) {
                    if (r.value().contains(alertId)
                            && r.value().contains(EventTypes.INCIDENT_DETECTED)) {
                        found = true;
                    }
                }
                assertThat(found).as("incident.detected containing %s", alertId).isTrue();
            });
        }

        // 3. Duplicate alert is ignored (still exactly one incident).
        template.send(KafkaTopics.TELEMETRY_ALERTS, alertId, envelope).get();
        // Give the consumer a moment to attempt processing.
        Thread.sleep(2_000);
        long count = incidents.findAllByOrderByDetectedAtDesc().stream()
                .filter(i -> alertId.equals(i.getExternalId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    private KafkaConsumer<String, String> downstreamConsumer(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
