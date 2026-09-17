-- Phase 7: remediation plans and execution audit. The LLM never writes these
-- rows as control flow; the incident-service validates policy first.

CREATE TABLE remediation_plans (
    id                 UUID PRIMARY KEY,
    incident_id        UUID          NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    action             VARCHAR(64)   NOT NULL,
    namespace          VARCHAR(64)   NOT NULL,
    deployment         VARCHAR(128)  NOT NULL,
    target_revision    INTEGER       NOT NULL,
    risk               VARCHAR(16)   NOT NULL,
    decision           VARCHAR(32)   NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    idempotency_key    VARCHAR(256)  NOT NULL UNIQUE,
    expected_impact    VARCHAR(512),
    blast_radius       VARCHAR(256),
    confidence         DOUBLE PRECISION,
    rationale          VARCHAR(1024),
    created_at         TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_remediation_plans_incident ON remediation_plans (incident_id, created_at DESC);

CREATE TABLE remediation_executions (
    id                 UUID PRIMARY KEY,
    incident_id        UUID          NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    plan_id            UUID          REFERENCES remediation_plans (id) ON DELETE SET NULL,
    idempotency_key    VARCHAR(256)  NOT NULL UNIQUE,
    status             VARCHAR(32)   NOT NULL,
    result_preview     VARCHAR(2048),
    error_message      VARCHAR(1024),
    started_at         TIMESTAMPTZ   NOT NULL,
    completed_at       TIMESTAMPTZ
);

CREATE INDEX idx_remediation_executions_incident ON remediation_executions (incident_id, started_at DESC);
