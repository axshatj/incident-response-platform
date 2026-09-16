package com.irp.incident.eventing;

/**
 * Well-known event-type strings. Keep this list aligned with SKILL.md and the
 * topic naming below.
 */
public final class EventTypes {

    private EventTypes() {}

    // Inbound
    public static final String TELEMETRY_ALERT = "telemetry.alert";

    // Outbound
    public static final String INCIDENT_DETECTED = "incident.detected";
    public static final String INCIDENT_UPDATED = "incident.updated";

    public static final String SOURCE_INCIDENT_SERVICE = "incident-service";
}
