package com.irp.incident.api;

import com.irp.incident.api.dto.AgentAdvanceRequest;
import com.irp.incident.api.dto.CreateIncidentRequest;
import com.irp.incident.api.dto.IncidentEventResponse;
import com.irp.incident.api.dto.IncidentResponse;
import com.irp.incident.api.dto.InvestigationReportRequest;
import com.irp.incident.api.dto.InvestigationView;
import com.irp.incident.api.dto.RemediationExecutionRequest;
import com.irp.incident.api.dto.RemediationPlanRequest;
import com.irp.incident.api.dto.RemediationView;
import com.irp.incident.api.dto.TransitionRequest;
import com.irp.incident.domain.Incident;
import com.irp.incident.service.IncidentService;
import com.irp.incident.service.InvestigationService;
import com.irp.incident.service.RemediationCoordinator;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private static final String ANONYMOUS_ACTOR = "anonymous";

    private final IncidentService incidents;
    private final InvestigationService investigations;
    private final RemediationCoordinator remediations;

    public IncidentController(IncidentService incidents,
                              InvestigationService investigations,
                              RemediationCoordinator remediations) {
        this.incidents = incidents;
        this.investigations = investigations;
        this.remediations = remediations;
    }

    @GetMapping
    public List<IncidentResponse> list() {
        return incidents.listAll().stream().map(IncidentResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        Incident created = incidents.create(
                request.externalId(),
                request.service(),
                request.title(),
                request.description(),
                request.severity(),
                request.environment()
        );
        return ResponseEntity
                .created(URI.create("/api/incidents/" + created.getId()))
                .body(IncidentResponse.from(created));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable UUID id) {
        return IncidentResponse.from(incidents.get(id));
    }

    @GetMapping("/{id}/events")
    public List<IncidentEventResponse> timeline(@PathVariable UUID id) {
        return incidents.timeline(id).stream().map(IncidentEventResponse::from).toList();
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentResponse acknowledge(@PathVariable UUID id,
                                        @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                incidents.acknowledge(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    @PostMapping("/{id}/approve")
    public IncidentResponse approve(@PathVariable UUID id,
                                    @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                remediations.approve(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    @PostMapping("/{id}/reject")
    public IncidentResponse reject(@PathVariable UUID id,
                                   @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                remediations.reject(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable UUID id,
                                    @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                incidents.resolve(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    /**
     * Agent-driven state machine advance. Still validated by
     * {@link com.irp.incident.domain.IncidentStatus}; this is not a bypass.
     */
    @PostMapping("/{id}/advance")
    public IncidentResponse advance(@PathVariable UUID id,
                                    @Valid @RequestBody AgentAdvanceRequest request) {
        return IncidentResponse.from(incidents.transition(
                id,
                request.target(),
                request.eventType(),
                request.actorOr("agent-service"),
                request.note()
        ));
    }

    @PostMapping("/{id}/investigations")
    public ResponseEntity<java.util.Map<String, UUID>> startInvestigation(@PathVariable UUID id) {
        var investigation = investigations.start(id);
        return ResponseEntity.ok(java.util.Map.of("investigationId", investigation.getId()));
    }

    @PostMapping("/{id}/investigation-report")
    public ResponseEntity<Void> recordInvestigation(@PathVariable UUID id,
                                                    @Valid @RequestBody InvestigationReportRequest report) {
        investigations.recordReport(id, report);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/investigation")
    public InvestigationView investigation(@PathVariable UUID id) {
        return investigations.view(id);
    }

    @PostMapping("/{id}/remediation-plan")
    public RemediationView proposeRemediation(@PathVariable UUID id,
                                              @Valid @RequestBody RemediationPlanRequest request) {
        return remediations.propose(id, request);
    }

    @GetMapping("/{id}/remediation")
    public RemediationView remediation(@PathVariable UUID id) {
        return remediations.view(id);
    }

    @PostMapping("/{id}/remediation-executions")
    public RemediationView recordExecution(@PathVariable UUID id,
                                           @Valid @RequestBody RemediationExecutionRequest request) {
        return remediations.recordExecution(id, request);
    }
}
