package com.irp.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.irp.agent.client.IncidentServiceClient;
import com.irp.agent.client.IncidentServiceClient.ExecutionDto;
import com.irp.agent.client.IncidentServiceClient.IncidentDto;
import com.irp.agent.client.IncidentServiceClient.RemediationViewDto;
import com.irp.agent.llm.StructuredLlm;
import com.irp.agent.schema.VerificationOutput;
import com.irp.agent.tools.StubOpsTools;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationOrchestratorTest {

    @Mock IncidentServiceClient incidents;
    @Mock StructuredLlm llm;

    private VerificationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new VerificationOrchestrator(incidents, llm, new StubOpsTools());
    }

    @Test
    void overlaysSuccessfulExecutionAndPostsLlmOutcome() {
        UUID id = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(
                id, "am-1", "payment-service", "DB pool exhausted", "timeouts",
                "SEV2", "VERIFYING", "prod"));
        when(incidents.getRemediation(id)).thenReturn(new RemediationViewDto(
                id,
                null,
                List.of(new ExecutionDto(UUID.randomUUID(), "SUCCEEDED", "rolled back r41"))
        ));
        when(llm.generate(any(), any(), eq(VerificationOutput.class))).thenReturn(
                new VerificationOutput("RESOLVED", "rollback restored pool headroom", List.of("error rate down")));

        orchestrator.handleVerifying(id);

        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(llm).generate(any(), user.capture(), eq(VerificationOutput.class));
        assertThat(user.getValue()).contains("DETERMINISTIC_RECOVERY: last execution SUCCEEDED");
        verify(incidents).postVerification(eq(id), eq("RESOLVED"), eq("rollback restored pool headroom"), any());
    }

    @Test
    void skipsWhenNotVerifying() {
        UUID id = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(
                id, "am-1", "payment-service", "title", "desc",
                "SEV2", "REMEDIATING", "prod"));

        orchestrator.handleVerifying(id);

        verify(incidents, never()).postVerification(any(), any(), any(), any());
    }

    @Test
    void overlayReportsMissingExecution() {
        assertThat(VerificationOrchestrator.recoveryOverlay(new RemediationViewDto(UUID.randomUUID(), null, List.of())))
                .contains("no SUCCEEDED execution");
    }
}
