package com.irp.agent.orchestrator;

import com.irp.agent.client.IncidentServiceClient;
import com.irp.agent.client.IncidentServiceClient.IncidentDto;
import com.irp.agent.client.IncidentServiceClient.RemediationViewDto;
import com.irp.agent.llm.StructuredLlm;
import com.irp.agent.observability.AgentMetrics;
import com.irp.agent.schema.VerificationOutput;
import com.irp.agent.tools.OpsTools;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Recovery checks after remediation. MCP stubs may still report the
 * pre-rollback failure; a successful execution overlay is the source of
 * truth the LLM is asked to consider, and incident-service still gates
 * RESOLVED on a SUCCEEDED execution.
 */
@Service
public class VerificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(VerificationOrchestrator.class);

    private static final String SYSTEM = """
            You are the Verification Agent. Decide if the incident recovered after remediation.
            Return ONLY JSON with keys outcome, summary, checks.
            outcome must be one of RESOLVED, PARTIALLY_RESOLVED, NOT_RESOLVED.
            Treat DETERMINISTIC_RECOVERY lines as authoritative over stale MCP snapshots.
            Do not include markdown.
            """;

    private static final List<String> CHECK_TOOLS = List.of(
            "query_metrics",
            "get_database_metrics",
            "get_service_health"
    );

    private final IncidentServiceClient incidents;
    private final StructuredLlm llm;
    private final OpsTools tools;
    private final AgentMetrics metrics;

    public VerificationOrchestrator(IncidentServiceClient incidents,
                                    StructuredLlm llm,
                                    OpsTools tools,
                                    AgentMetrics metrics) {
        this.incidents = incidents;
        this.llm = llm;
        this.tools = tools;
        this.metrics = metrics;
    }

    public void handleVerifying(UUID incidentId) {
        IncidentDto incident = incidents.get(incidentId);
        if (!"VERIFYING".equals(incident.status())) {
            log.info("Skipping verification for {} in status {}", incidentId, incident.status());
            return;
        }

        RemediationViewDto remediation = incidents.getRemediation(incidentId);
        List<String> signals = new ArrayList<>();
        signals.add(recoveryOverlay(remediation));
        for (String tool : CHECK_TOOLS) {
            if (!tools.allowlist().contains(tool)) {
                continue;
            }
            try {
                signals.add(tool + ": " + truncate(tools.invoke(tool, incident.service())));
                metrics.recordTool(tool, true);
            } catch (RuntimeException e) {
                signals.add(tool + ": FAILED " + e.getMessage());
                metrics.recordTool(tool, false);
            }
        }

        VerificationOutput out = llm.generate(SYSTEM, """
                incidentId=%s
                service=%s
                title=%s
                signals:
                %s
                """.formatted(incident.id(), incident.service(), incident.title(), String.join("\n", signals)),
                VerificationOutput.class);

        List<String> posted = new ArrayList<>(signals);
        posted.addAll(out.checks());
        incidents.postVerification(incidentId, out.outcome(), out.summary(), posted);
        metrics.recordRun("VERIFICATION", true);
        log.info("Verification posted for {} outcome={}", incidentId, out.outcome());
    }

    static String recoveryOverlay(RemediationViewDto remediation) {
        if (remediation == null || remediation.executions() == null) {
            return "DETERMINISTIC_RECOVERY: no SUCCEEDED execution recorded.";
        }
        return remediation.executions().stream()
                .filter(execution -> "SUCCEEDED".equals(execution.status()))
                .findFirst()
                .map(execution -> "DETERMINISTIC_RECOVERY: last execution SUCCEEDED. "
                        + (execution.resultPreview() == null ? "" : execution.resultPreview()))
                .orElse("DETERMINISTIC_RECOVERY: no SUCCEEDED execution recorded.");
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
