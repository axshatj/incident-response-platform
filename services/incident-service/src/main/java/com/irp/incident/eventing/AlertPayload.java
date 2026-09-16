package com.irp.incident.eventing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.irp.incident.domain.Severity;
import java.util.Map;

/**
 * Payload of the {@code telemetry.alert} envelope. {@code alertId} is required
 * and doubles as the incident's {@code external_id}, giving us exactly-once
 * semantics at the incident level via the unique constraint in the DB.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertPayload(
        String alertId,
        String service,
        String title,
        String description,
        Severity severity,
        String environment,
        Map<String, String> labels
) {}
