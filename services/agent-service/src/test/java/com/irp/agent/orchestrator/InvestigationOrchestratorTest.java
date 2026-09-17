package com.irp.agent.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.irp.agent.client.IncidentServiceClient;
import com.irp.agent.client.IncidentServiceClient.IncidentDto;
import com.irp.agent.client.IncidentServiceClient.InvestigationReport;
import com.irp.agent.llm.StructuredLlm;
import com.irp.agent.schema.InvestigationOutput;
import com.irp.agent.schema.RootCauseOutput;
import com.irp.agent.schema.TriageOutput;
import com.irp.agent.tools.OpsToolCatalog;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestigationOrchestratorTest {

    @Mock IncidentServiceClient incidents;
    @Mock StructuredLlm llm;

    private InvestigationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new InvestigationOrchestrator(incidents, llm, new OpsToolCatalog(), 4, 10_000);
    }

    @Test
    void detectedIncidentIsTriagedInvestigatedAndRcaRecorded() {
        UUID id = UUID.randomUUID();
        when(incidents.get(id)).thenReturn(new IncidentDto(
                id, "am-1", "payment-service", "DB pool exhausted", "timeouts",
                "SEV2", "DETECTED", "prod"));
        when(incidents.startInvestigation(id)).thenReturn(UUID.randomUUID());
        when(llm.generate(any(), any(), eq(TriageOutput.class))).thenReturn(new TriageOutput(
                "SEV2",
                List.of("payment-service"),
                List.of("query_metrics", "shell", "get_database_metrics")
        ));
        when(llm.generate(any(), any(), eq(InvestigationOutput.class)))
                .thenReturn(new InvestigationOutput("enough evidence", true));
        when(llm.generate(any(), any(), eq(RootCauseOutput.class))).thenReturn(new RootCauseOutput(
                "pool exhaustion after v42",
                0.9,
                List.of("pool saturated"),
                List.of(),
                List.of("payment-service")
        ));

        orchestrator.handleDetected(id);

        verify(incidents).advance(eq(id), eq("TRIAGING"), eq("TRIAGE_STARTED"), eq("triage-agent"), any());
        verify(incidents).advance(eq(id), eq("INVESTIGATING"), eq("INVESTIGATION_STARTED"), eq("investigation-agent"), any());
        verify(incidents).advance(eq(id), eq("ROOT_CAUSE_IDENTIFIED"), eq("RCA_GENERATED"), eq("rca-agent"), any());

        ArgumentCaptor<InvestigationReport> report = ArgumentCaptor.forClass(InvestigationReport.class);
        verify(incidents).postReport(eq(id), report.capture());
        assertThat(report.getValue().observations()).hasSize(2); // shell stripped from plan
        assertThat(report.getValue().observations()).noneMatch(o -> "SHELL".equals(o.type()));
    }
}
