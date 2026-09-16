package com.irp.incident.eventing;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka topology and error handling.
 *
 * Retry policy: on any exception in a listener the {@link DefaultErrorHandler}
 * retries {@link #RETRY_ATTEMPTS} times with a fixed {@link #RETRY_BACKOFF_MS}
 * pause, then publishes the record to {@code <topic>.DLT} via the
 * {@link DeadLetterPublishingRecoverer}. The DLT preserves the original
 * partition so alert-to-DLT mapping stays deterministic.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    private static final long RETRY_BACKOFF_MS = 2_000L;
    private static final long RETRY_ATTEMPTS = 3L;

    @Value("${irp.kafka.partitions:3}")
    private int partitions;

    @Value("${irp.kafka.replicas:1}")
    private short replicas;

    @Bean
    NewTopic telemetryAlertsTopic() {
        return TopicBuilder.name(KafkaTopics.TELEMETRY_ALERTS)
                .partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic telemetryAlertsDltTopic() {
        return TopicBuilder.name(KafkaTopics.TELEMETRY_ALERTS_DLT)
                .partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic incidentDetectedTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_DETECTED)
                .partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic incidentUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_UPDATED)
                .partitions(partitions).replicas(replicas).build();
    }

    @Bean
    DefaultErrorHandler defaultErrorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (ConsumerRecord<?, ?> record, Exception ex) -> {
                    log.warn("Routing record from {} partition {} offset {} to DLT after {} failures: {}",
                            record.topic(), record.partition(), record.offset(),
                            RETRY_ATTEMPTS + 1, ex.getMessage());
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_BACKOFF_MS, RETRY_ATTEMPTS));
    }
}
