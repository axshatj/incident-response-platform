package com.irp.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OpsToolCatalogTest {

    private final OpsToolCatalog catalog = new OpsToolCatalog();

    @Test
    void allowlistedToolsReturnCompactEvidence() {
        String metrics = catalog.invoke("query_metrics", "payment-service");
        assertThat(metrics).contains("payment-service").contains("db.pool.active");
        assertThat(metrics.length()).isLessThan(2000);
    }

    @Test
    void shellAndKubectlAreRejected() {
        assertThatThrownBy(() -> catalog.invoke("shell", "payment-service"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowlisted");
        assertThatThrownBy(() -> catalog.invoke("kubectl", "payment-service"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
