package com.irp.incident.api;

import com.irp.incident.api.dto.CreateIncidentRequest;
import com.irp.incident.api.dto.IncidentEventResponse;
import com.irp.incident.api.dto.IncidentResponse;
import com.irp.incident.api.dto.TransitionRequest;
import com.irp.incident.domain.Incident;
import com.irp.incident.service.IncidentService;
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

    public IncidentController(IncidentService incidents) {
        this.incidents = incidents;
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
                incidents.approve(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    @PostMapping("/{id}/reject")
    public IncidentResponse reject(@PathVariable UUID id,
                                   @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                incidents.reject(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable UUID id,
                                    @Valid @RequestBody(required = false) TransitionRequest request) {
        TransitionRequest safe = request == null ? new TransitionRequest(null, null) : request;
        return IncidentResponse.from(
                incidents.resolve(id, safe.actorOr(ANONYMOUS_ACTOR), safe.note()));
    }
}
