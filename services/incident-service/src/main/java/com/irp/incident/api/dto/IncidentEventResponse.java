package com.irp.incident.api.dto;

import com.irp.incident.domain.IncidentEvent;
import com.irp.incident.domain.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

public record IncidentEventResponse(
        UUID id,
        UUID incidentId,
        String eventType,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        String actor,
        String note,
        Instant occurredAt
) {
    public static IncidentEventResponse from(IncidentEvent event) {
        return new IncidentEventResponse(
                event.getId(),
                event.getIncidentId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getActor(),
                event.getNote(),
                event.getOccurredAt()
        );
    }
}
