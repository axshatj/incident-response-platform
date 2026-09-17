package com.irp.incident.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.incident.api.dto.InvestigationReportRequest;
import com.irp.incident.api.dto.InvestigationReportRequest.AgentRunRequest;
import com.irp.incident.api.dto.InvestigationReportRequest.ObservationRequest;
import com.irp.incident.api.dto.InvestigationReportRequest.RootCauseRequest;
import com.irp.incident.api.dto.InvestigationReportRequest.ToolCallRequest;
import com.irp.incident.api.dto.InvestigationView;
import com.irp.incident.api.dto.InvestigationView.AgentRunView;
import com.irp.incident.api.dto.InvestigationView.ObservationView;
import com.irp.incident.api.dto.InvestigationView.RootCauseView;
import com.irp.incident.domain.AgentRun;
import com.irp.incident.domain.Incident;
import com.irp.incident.domain.Investigation;
import com.irp.incident.domain.Observation;
import com.irp.incident.domain.RootCause;
import com.irp.incident.domain.ToolCall;
import com.irp.incident.repository.AgentRunRepository;
import com.irp.incident.repository.InvestigationRepository;
import com.irp.incident.repository.ObservationRepository;
import com.irp.incident.repository.RootCauseRepository;
import com.irp.incident.repository.ToolCallRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestigationService {

    private final IncidentService incidents;
    private final InvestigationRepository investigations;
    private final ObservationRepository observations;
    private final AgentRunRepository agentRuns;
    private final ToolCallRepository toolCalls;
    private final RootCauseRepository rootCauses;
    private final ObjectMapper mapper;
    private final Clock clock;

    public InvestigationService(IncidentService incidents,
                                InvestigationRepository investigations,
                                ObservationRepository observations,
                                AgentRunRepository agentRuns,
                                ToolCallRepository toolCalls,
                                RootCauseRepository rootCauses,
                                ObjectMapper mapper,
                                Clock clock) {
        this.incidents = incidents;
        this.investigations = investigations;
        this.observations = observations;
        this.agentRuns = agentRuns;
        this.toolCalls = toolCalls;
        this.rootCauses = rootCauses;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public Investigation start(UUID incidentId) {
        incidents.get(incidentId);
        Investigation investigation = new Investigation(
                UUID.randomUUID(), incidentId, "RUNNING", clock.instant());
        return investigations.save(investigation);
    }

    @Transactional
    public void recordReport(UUID incidentId, InvestigationReportRequest report) {
        Instant now = clock.instant();
        Investigation investigation = investigations.findById(report.investigationId())
                .orElseGet(() -> investigations.save(
                        new Investigation(report.investigationId(), incidentId, "RUNNING", now)));
        if (report.observations() != null) {
            for (ObservationRequest o : report.observations()) {
                observations.save(new Observation(
                        UUID.randomUUID(),
                        incidentId,
                        investigation.getId(),
                        o.type(),
                        o.source(),
                        o.observedAt() == null ? now : o.observedAt(),
                        o.content(),
                        o.confidence(),
                        o.traceId()
                ));
            }
        }
        if (report.agentRuns() != null) {
            for (AgentRunRequest runReq : report.agentRuns()) {
                AgentRun run = new AgentRun(
                        runReq.id(),
                        incidentId,
                        runReq.agentType(),
                        runReq.model(),
                        runReq.status(),
                        runReq.startedAt() == null ? now : runReq.startedAt()
                );
                long latency = runReq.latencyMs() == null ? 0L : runReq.latencyMs();
                Instant completedAt = runReq.completedAt() == null ? now : runReq.completedAt();
                if ("FAILED".equals(runReq.status()) || (runReq.errorMessage() != null && !runReq.errorMessage().isBlank())) {
                    run.fail(runReq.errorMessage() == null ? "agent run failed" : runReq.errorMessage(),
                            latency, completedAt);
                } else {
                    run.complete(runReq.status(), runReq.inputTokens(), runReq.outputTokens(), latency, completedAt);
                }
                agentRuns.save(run);
                if (runReq.toolCalls() != null) {
                    for (ToolCallRequest tc : runReq.toolCalls()) {
                        toolCalls.save(new ToolCall(
                                UUID.randomUUID(),
                                run.getId(),
                                tc.toolName(),
                                tc.argumentsHash(),
                                tc.status(),
                                tc.latencyMs(),
                                tc.resultPreview(),
                                tc.startedAt() == null ? now : tc.startedAt(),
                                tc.completedAt()
                        ));
                    }
                }
            }
        }
        if (report.rootCause() != null) {
            RootCauseRequest rc = report.rootCause();
            RootCause saved = rootCauses.save(new RootCause(
                    UUID.randomUUID(),
                    incidentId,
                    rc.statement(),
                    rc.confidence(),
                    writeJson(rc.evidence() == null ? List.of() : rc.evidence()),
                    writeJson(rc.counterEvidence() == null ? List.of() : rc.counterEvidence()),
                    writeJson(rc.affectedComponents() == null ? List.of() : rc.affectedComponents()),
                    now
            ));
            Incident incident = incidents.get(incidentId);
            incident.attachRootCause(saved.getId());
        }
        investigation.complete(report.summary(), now);
    }

    @Transactional(readOnly = true)
    public InvestigationView view(UUID incidentId) {
        incidents.get(incidentId);
        List<ObservationView> obs = observations.findByIncidentIdOrderByObservedAtAsc(incidentId)
                .stream().map(ObservationView::from).toList();
        List<AgentRunView> runs = agentRuns.findByIncidentIdOrderByStartedAtAsc(incidentId)
                .stream()
                .map(run -> AgentRunView.from(run, toolCalls.findByAgentRunIdOrderByStartedAtAsc(run.getId())))
                .toList();
        RootCauseView rca = rootCauses.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream().findFirst().map(RootCauseView::from).orElse(null);
        return new InvestigationView(incidentId, obs, runs, rca);
    }

    private String writeJson(List<String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize investigation JSON", e);
        }
    }
}
