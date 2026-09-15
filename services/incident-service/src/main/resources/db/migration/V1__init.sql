-- Incident aggregate + immutable audit trail.
-- Phase 1 schema. Later phases will add services, deployments, agents, etc.

CREATE TABLE incidents (
    id            UUID PRIMARY KEY,
    external_id   VARCHAR(128) UNIQUE,
    service       VARCHAR(128) NOT NULL,
    title         VARCHAR(256) NOT NULL,
    description   TEXT,
    severity      VARCHAR(16)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    environment   VARCHAR(64)  NOT NULL,
    detected_at   TIMESTAMPTZ  NOT NULL,
    resolved_at   TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_incidents_status         ON incidents (status);
CREATE INDEX idx_incidents_detected_at    ON incidents (detected_at DESC);
CREATE INDEX idx_incidents_service_status ON incidents (service, status);

CREATE TABLE incident_events (
    id           UUID PRIMARY KEY,
    incident_id  UUID         NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    event_type   VARCHAR(64)  NOT NULL,
    from_status  VARCHAR(32),
    to_status    VARCHAR(32),
    actor        VARCHAR(128),
    note         VARCHAR(1024),
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_incident_events_incident_id ON incident_events (incident_id, occurred_at);
