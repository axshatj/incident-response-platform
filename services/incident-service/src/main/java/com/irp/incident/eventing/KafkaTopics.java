package com.irp.incident.eventing;

/** Canonical Kafka topic names. Keep in sync with SKILL.md. */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String TELEMETRY_ALERTS = "telemetry.alerts";
    public static final String TELEMETRY_ALERTS_DLT = "telemetry.alerts.DLT";

    public static final String INCIDENT_DETECTED = "incident.detected";
    public static final String INCIDENT_UPDATED = "incident.updated";
}
