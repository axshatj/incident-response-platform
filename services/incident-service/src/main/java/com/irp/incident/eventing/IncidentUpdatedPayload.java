package com.irp.incident.eventing;

import com.irp.incident.domain.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

public record IncidentUpdatedPayload(
        UUID incidentId,
        String externalId,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        String eventType,
        String actor,
        Instant occurredAt
) {}
