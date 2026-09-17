package com.irp.remediation.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SimulatedKubernetesClientTest {

    private final SimulatedKubernetesClient k8s = new SimulatedKubernetesClient();

    @Test
    void rollsBackPaymentServiceToPreviousRevision() {
        String result = k8s.rollback("prod", "payment-service", 41);
        assertThat(result).contains("42 -> 41");
        assertThat(k8s.currentRevision("prod", "payment-service")).isEqualTo(41);
    }

    @Test
    void rejectsUnknownNamespaceAndInvalidRevision() {
        assertThatThrownBy(() -> k8s.rollback("kube-system", "payment-service", 41))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Namespace not allowlisted");
        assertThatThrownBy(() -> k8s.rollback("prod", "payment-service", 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid target revision");
        assertThatThrownBy(() -> k8s.rollback("prod", "other-service", 41))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deployment not allowlisted");
    }
}
