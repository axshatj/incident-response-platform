package com.irp.incident.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgeSeederTest {

    @Test
    void parseReadsFrontMatterAndBody() {
        String markdown = """
                ---
                source: runbook
                service: payment-service
                title: Pool exhaustion
                tags: hikari,postgres
                ---

                Roll back the payment-service deploy.
                """;
        KnowledgeSeeder.ParsedDocument doc = KnowledgeSeeder.parse(
                "knowledge/runbooks/payment-db-pool.md", markdown);

        assertThat(doc.source()).isEqualTo("runbook");
        assertThat(doc.service()).isEqualTo("payment-service");
        assertThat(doc.title()).isEqualTo("Pool exhaustion");
        assertThat(doc.tags()).isEqualTo("hikari,postgres");
        assertThat(doc.content()).contains("Roll back");
        assertThat(doc.path()).isEqualTo("knowledge/runbooks/payment-db-pool.md");
    }
}
