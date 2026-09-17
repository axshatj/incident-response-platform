package com.irp.agent.orchestrator;

import com.irp.agent.client.IncidentServiceClient;
import com.irp.agent.client.IncidentServiceClient.AgentRunDto;
import com.irp.agent.client.IncidentServiceClient.IncidentDto;
import com.irp.agent.client.IncidentServiceClient.InvestigationReport;
import com.irp.agent.client.IncidentServiceClient.ObservationDto;
import com.irp.agent.client.IncidentServiceClient.RootCauseDto;
import com.irp.agent.client.IncidentServiceClient.ToolCallDto;
import com.irp.agent.llm.StructuredLlm;
import com.irp.agent.schema.InvestigationOutput;
import com.irp.agent.schema.RootCauseOutput;
import com.irp.agent.schema.TriageOutput;
import com.irp.agent.tools.OpsToolCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs the Phase 4 loop: triage → bounded tool investigation → RCA hypothesis.
 * Tools are invoked deterministically first; the LLM only sees compact evidence.
 */
@Service
public class InvestigationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestrator.class);

    private static final String TRIAGE_SYSTEM = """
            You are the Triage Agent for an incident-response platform.
            Return ONLY JSON with keys severity, suspectedServices, investigationPlan.
            severity must be one of SEV1, SEV2, SEV3, SEV4.
            investigationPlan values must be tool names from:
            query_metrics, query_logs, query_traces, get_deployment_history, get_database_metrics, get_service_health.
            Do not invent tools. Do not include markdown.
            """;

    private static final String INVESTIGATION_SYSTEM = """
            You are the Investigation Agent. You are given compact evidence collected by tools.
            Return ONLY JSON with keys summary, sufficientEvidence.
            Do not request more raw logs. Do not include markdown.
            """;

    private static final String RCA_SYSTEM = """
            You are the Root Cause Agent. Use only the supplied evidence.
            Return ONLY JSON with keys rootCause, confidence, evidence, counterEvidence, affectedComponents.
            Never present unsupported guesses as facts. If evidence is weak, lower confidence and populate counterEvidence.
            Do not include markdown.
            """;

    private final IncidentServiceClient incidents;
    private final StructuredLlm llm;
    private final OpsToolCatalog tools;

    private final int maxToolCalls;
    private final long maxLatencyMs;

    public InvestigationOrchestrator(IncidentServiceClient incidents,
                                     StructuredLlm llm,
                                     OpsToolCatalog tools,
                                     @Value("${irp.agent.budget.max-tool-calls}") int maxToolCalls,
                                     @Value("${irp.agent.budget.max-latency-ms}") long maxLatencyMs) {
        this.incidents = incidents;
        this.llm = llm;
        this.tools = tools;
        this.maxToolCalls = maxToolCalls;
        this.maxLatencyMs = maxLatencyMs;
    }

    public void handleDetected(UUID incidentId) {
        IncidentDto incident = incidents.get(incidentId);
        if (!"DETECTED".equals(incident.status())) {
            log.info("Skipping incident {} in status {}", incidentId, incident.status());
            return;
        }
        long started = System.currentTimeMillis();
        List<AgentRunDto> runs = new ArrayList<>();
        List<ObservationDto> observations = new ArrayList<>();

        incidents.advance(incidentId, "TRIAGING", "TRIAGE_STARTED", "triage-agent", "auto-triage");
        TriageOutput triage = runTriage(incident, runs);

        UUID investigationId = incidents.startInvestigation(incidentId);
        incidents.advance(incidentId, "INVESTIGATING", "INVESTIGATION_STARTED", "investigation-agent",
                IncidentServiceClient.noteFor(triage));

        String evidenceBundle = runTools(incident, triage, observations, runs, started);
        InvestigationOutput investigation = runInvestigation(incident, evidenceBundle, runs);
        RootCauseOutput rca = runRca(incident, evidenceBundle, runs);

        incidents.postReport(incidentId, new InvestigationReport(
                investigationId,
                investigation.summary(),
                observations,
                runs,
                RootCauseDto.from(rca)
        ));
        incidents.advance(incidentId, "ROOT_CAUSE_IDENTIFIED", "RCA_GENERATED", "rca-agent", rca.rootCause());
        log.info("Investigation complete for {} confidence={}", incidentId, rca.confidence());
    }

    private TriageOutput runTriage(IncidentDto incident, List<AgentRunDto> runs) {
        Instant t0 = Instant.now();
        long nano = System.nanoTime();
        UUID runId = UUID.randomUUID();
        try {
            TriageOutput out = llm.generate(TRIAGE_SYSTEM, """
                    service=%s
                    title=%s
                    description=%s
                    reportedSeverity=%s
                    environment=%s
                    """.formatted(incident.service(), incident.title(),
                    incident.description(), incident.severity(), incident.environment()),
                    TriageOutput.class);
            out = sanitizePlan(out);
            runs.add(completedRun(runId, "TRIAGE", t0, nano, List.of()));
            return out;
        } catch (RuntimeException e) {
            runs.add(failedRun(runId, "TRIAGE", t0, nano, e.getMessage()));
            throw e;
        }
    }

    private String runTools(IncidentDto incident,
                            TriageOutput triage,
                            List<ObservationDto> observations,
                            List<AgentRunDto> runs,
                            long orchestratorStartMs) {
        Instant t0 = Instant.now();
        long nano = System.nanoTime();
        UUID runId = UUID.randomUUID();
        List<ToolCallDto> calls = new ArrayList<>();
        StringBuilder evidence = new StringBuilder();
        int used = 0;
        try {
            for (String tool : triage.investigationPlan()) {
                if (used >= maxToolCalls) {
                    break;
                }
                if (System.currentTimeMillis() - orchestratorStartMs > maxLatencyMs) {
                    log.warn("Investigation budget exhausted (latency) for {}", incident.id());
                    break;
                }
                Instant callStart = Instant.now();
                long callNano = System.nanoTime();
                String preview;
                String status = "OK";
                try {
                    preview = tools.invoke(tool, incident.service());
                } catch (RuntimeException e) {
                    preview = e.getMessage();
                    status = "FAILED";
                }
                long latencyMs = (System.nanoTime() - callNano) / 1_000_000L;
                Instant callEnd = Instant.now();
                calls.add(new ToolCallDto(
                        tool,
                        hash(incident.service() + ":" + tool),
                        status,
                        latencyMs,
                        truncate(preview),
                        callStart,
                        callEnd
                ));
                if ("OK".equals(status)) {
                    observations.add(new ObservationDto(
                            tool.toUpperCase(),
                            tool,
                            callEnd,
                            preview,
                            0.8,
                            null
                    ));
                    evidence.append("- ").append(tool).append(": ").append(preview).append('\n');
                }
                used++;
            }
            runs.add(completedRun(runId, "INVESTIGATION_TOOLS", t0, nano, calls));
            return evidence.toString();
        } catch (RuntimeException e) {
            runs.add(failedRun(runId, "INVESTIGATION_TOOLS", t0, nano, e.getMessage()));
            throw e;
        }
    }

    private InvestigationOutput runInvestigation(IncidentDto incident, String evidence, List<AgentRunDto> runs) {
        Instant t0 = Instant.now();
        long nano = System.nanoTime();
        UUID runId = UUID.randomUUID();
        try {
            InvestigationOutput out = llm.generate(INVESTIGATION_SYSTEM, """
                    incidentId=%s
                    service=%s
                    evidence:
                    %s
                    """.formatted(incident.id(), incident.service(), evidence), InvestigationOutput.class);
            runs.add(completedRun(runId, "INVESTIGATION", t0, nano, List.of()));
            return out;
        } catch (RuntimeException e) {
            runs.add(failedRun(runId, "INVESTIGATION", t0, nano, e.getMessage()));
            throw e;
        }
    }

    private RootCauseOutput runRca(IncidentDto incident, String evidence, List<AgentRunDto> runs) {
        Instant t0 = Instant.now();
        long nano = System.nanoTime();
        UUID runId = UUID.randomUUID();
        try {
            RootCauseOutput out = llm.generate(RCA_SYSTEM, """
                    incidentId=%s
                    service=%s
                    evidence:
                    %s
                    """.formatted(incident.id(), incident.service(), evidence), RootCauseOutput.class);
            runs.add(completedRun(runId, "RCA", t0, nano, List.of()));
            return out;
        } catch (RuntimeException e) {
            runs.add(failedRun(runId, "RCA", t0, nano, e.getMessage()));
            throw e;
        }
    }

    private TriageOutput sanitizePlan(TriageOutput triage) {
        List<String> plan = triage.investigationPlan().stream()
                .map(String::trim)
                .filter(OpsToolCatalog.ALLOWLIST::contains)
                .distinct()
                .limit(maxToolCalls)
                .toList();
        if (plan.isEmpty()) {
            plan = List.of("query_metrics", "query_logs", "get_deployment_history", "get_database_metrics");
        }
        return new TriageOutput(triage.severity(), triage.suspectedServices(), plan);
    }

    private static AgentRunDto completedRun(UUID id, String type, Instant started, long nanoStart, List<ToolCallDto> tools) {
        Instant done = Instant.now();
        return new AgentRunDto(id, type, "stub", "COMPLETED", null, null,
                (System.nanoTime() - nanoStart) / 1_000_000L, null, started, done, tools);
    }

    private static AgentRunDto failedRun(UUID id, String type, Instant started, long nanoStart, String error) {
        Instant done = Instant.now();
        return new AgentRunDto(id, type, "stub", "FAILED", null, null,
                (System.nanoTime() - nanoStart) / 1_000_000L, error, started, done, List.of());
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
