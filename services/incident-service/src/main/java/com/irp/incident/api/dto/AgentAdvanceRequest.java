package com.irp.incident.api.dto;

import com.irp.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentAdvanceRequest(
        @NotNull IncidentStatus target,
        @NotBlank @Size(max = 64) String eventType,
        @Size(max = 128) String actor,
        @Size(max = 1024) String note
) {
    public String actorOr(String fallback) {
        return actor == null || actor.isBlank() ? fallback : actor;
    }
}
