package com.irp.incident.eventing;

import com.irp.incident.domain.Severity;
import java.time.Instant;
import java.util.UUID;

public record IncidentDetectedPayload(
        UUID incidentId,
        String externalId,
        String service,
        String title,
        Severity severity,
        String environment,
        Instant detectedAt
) {}
