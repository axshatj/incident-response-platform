package com.irp.incident.api.dto;

import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentStatus;
import com.irp.incident.domain.Severity;
import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String externalId,
        String service,
        String title,
        String description,
        Severity severity,
        IncidentStatus status,
        String environment,
        Instant detectedAt,
        Instant resolvedAt,
        UUID rootCauseId
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getExternalId(),
                incident.getService(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getEnvironment(),
                incident.getDetectedAt(),
                incident.getResolvedAt(),
                incident.getRootCauseId()
        );
    }
}
