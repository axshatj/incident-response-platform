package com.irp.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IncidentOpsToolsTest {

    private final IncidentOpsTools tools = new IncidentOpsTools();

    @Test
    void allowlistedToolsReturnCompactEvidence() {
        String metrics = tools.invoke("query_metrics", "payment-service");
        assertThat(metrics).contains("payment-service").contains("db.pool.active");
        assertThat(metrics.length()).isLessThan(2000);
    }

    @Test
    void shellAndKubectlAreRejected() {
        assertThatThrownBy(() -> tools.invoke("shell", "payment-service"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowlisted");
        assertThatThrownBy(() -> tools.invoke("kubectl", "payment-service"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
