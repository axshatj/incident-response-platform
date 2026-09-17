package com.irp.incident.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.irp.incident.observability.IncidentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KnowledgeServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    private KnowledgeService knowledge;

    @BeforeEach
    void setUp() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());

        knowledge = new KnowledgeService(
                new JdbcTemplate(ds),
                new HashingEmbedder(),
                new IncidentMetrics(new SimpleMeterRegistry()),
                Clock.systemUTC()
        );
        new KnowledgeSeeder(knowledge).seedClasspath();
    }

    @Test
    void paymentPoolQueryRetrievesPaymentRunbookNotKafkaLag() {
        List<KnowledgeHit> hits = knowledge.search(
                "payment-service hikari postgres connection pool exhaustion after deploy",
                "payment-service",
                "prod",
                3
        );

        assertThat(hits).isNotEmpty();
        assertThat(hits.stream().map(KnowledgeHit::path).toList())
                .anyMatch(path -> path.contains("payment-db-pool") || path.contains("INC-2025-0412"));
        assertThat(hits.getFirst().path()).doesNotContain("kafka-consumer-lag");
        assertThat(hits.getFirst().citation()).contains("[");
    }
}
