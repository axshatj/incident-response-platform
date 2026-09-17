package com.irp.incident.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashingEmbedderTest {

    private final HashingEmbedder embedder = new HashingEmbedder();

    @Test
    void overlappingOpsTermsRankHigherThanUnrelatedTopic() {
        float[] query = embedder.embed("payment-service hikari postgres connection pool exhaustion");
        float[] runbook = embedder.embed(
                "Payment service database connection exhaustion hikari postgres max_connections");
        float[] kafkaLag = embedder.embed("Kafka consumer lag on notification-service");

        assertThat(query).hasSize(HashingEmbedder.DIMENSIONS);
        assertThat(HashingEmbedder.cosine(query, runbook))
                .isGreaterThan(HashingEmbedder.cosine(query, kafkaLag));
    }

    @Test
    void pgVectorLiteralIsParseable() {
        String literal = HashingEmbedder.toPgVector(embedder.embed("postgres"));
        assertThat(literal).startsWith("[").endsWith("]");
        assertThat(literal.split(",")).hasSize(HashingEmbedder.DIMENSIONS);
    }
}
