package com.irp.remediation.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.irp.remediation.client.IncidentServiceClient;
import com.irp.remediation.client.IncidentServiceClient.ExecutionRequest;
import com.irp.remediation.client.IncidentServiceClient.IncidentDto;
import com.irp.remediation.client.IncidentServiceClient.PlanView;
import com.irp.remediation.client.IncidentServiceClient.RemediationView;
import com.irp.remediation.k8s.SimulatedKubernetesClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemediationExecutorTest {

    @Mock IncidentServiceClient incidents;

    private RemediationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new RemediationExecutor(incidents, new SimulatedKubernetesClient(), new SimpleMeterRegistry());
    }

    @Test
    void approvedRollbackIsExecuted() {
        UUID id = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(id, "payment-service", "REMEDIATING", "prod"));
        when(incidents.remediation(id)).thenReturn(new RemediationView(id, new PlanView(
                planId, "ROLLBACK_DEPLOYMENT", "prod", "payment-service", 41,
                "HIGH", "REQUIRE_APPROVAL", "APPROVED", id + ":ROLLBACK_DEPLOYMENT:prod:payment-service:41"
        ), List.of()));

        executor.executeIfRemediating(id);

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(incidents).recordExecution(eq(id), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("SUCCEEDED");
        assertThat(captor.getValue().resultPreview()).contains("42 -> 41");
    }

    @Test
    void shellPlanIsRefused() {
        UUID id = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(id, "payment-service", "REMEDIATING", "prod"));
        when(incidents.remediation(id)).thenReturn(new RemediationView(id, new PlanView(
                UUID.randomUUID(), "SHELL", "prod", "payment-service", 41,
                "CRITICAL", "PROHIBITED", "APPROVED", "k"
        ), List.of()));

        executor.executeIfRemediating(id);

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(incidents).recordExecution(eq(id), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("FAILED");
        assertThat(captor.getValue().errorMessage()).contains("prohibited");
    }

    @Test
    void skipsWhenNotRemediating() {
        UUID id = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(id, "payment-service", "AWAITING_APPROVAL", "prod"));

        executor.executeIfRemediating(id);

        verify(incidents, never()).recordExecution(any(), any());
    }
}
