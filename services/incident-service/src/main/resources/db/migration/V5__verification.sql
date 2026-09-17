-- Phase 8: verification attempts and audit. Retry budget is owned by
-- incident-service, not the LLM.

ALTER TABLE incidents
    ADD COLUMN verification_attempts INTEGER NOT NULL DEFAULT 0;

CREATE TABLE verification_runs (
    id           UUID PRIMARY KEY,
    incident_id  UUID         NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    attempt      INTEGER      NOT NULL,
    outcome      VARCHAR(32)  NOT NULL,
    summary      VARCHAR(1024),
    signals_json TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_verification_runs_incident ON verification_runs (incident_id, created_at DESC);
