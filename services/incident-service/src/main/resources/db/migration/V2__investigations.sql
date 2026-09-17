-- Phase 4: investigation artifacts. PostgreSQL remains the source of truth
-- for agent runs, tool calls, observations and root-cause records.

ALTER TABLE incidents
    ADD COLUMN root_cause_id UUID;

CREATE TABLE investigations (
    id             UUID PRIMARY KEY,
    incident_id    UUID         NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    status         VARCHAR(32)  NOT NULL,
    summary        TEXT,
    started_at     TIMESTAMPTZ  NOT NULL,
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_investigations_incident_id ON investigations (incident_id);

CREATE TABLE observations (
    id                UUID PRIMARY KEY,
    incident_id       UUID         NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    investigation_id  UUID         REFERENCES investigations (id) ON DELETE SET NULL,
    type              VARCHAR(64)  NOT NULL,
    source            VARCHAR(128) NOT NULL,
    observed_at       TIMESTAMPTZ  NOT NULL,
    content           TEXT         NOT NULL,
    confidence        DOUBLE PRECISION,
    trace_id          VARCHAR(64)
);

CREATE INDEX idx_observations_incident_id ON observations (incident_id, observed_at);

CREATE TABLE agent_runs (
    id             UUID PRIMARY KEY,
    incident_id    UUID         NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    agent_type     VARCHAR(64)  NOT NULL,
    model          VARCHAR(128),
    status         VARCHAR(32)  NOT NULL,
    input_tokens   INTEGER,
    output_tokens  INTEGER,
    latency_ms     BIGINT,
    error_message  VARCHAR(1024),
    started_at     TIMESTAMPTZ  NOT NULL,
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_agent_runs_incident_id ON agent_runs (incident_id, started_at);

CREATE TABLE tool_calls (
    id              UUID PRIMARY KEY,
    agent_run_id    UUID         NOT NULL REFERENCES agent_runs (id) ON DELETE CASCADE,
    tool_name       VARCHAR(128) NOT NULL,
    arguments_hash  VARCHAR(64),
    status          VARCHAR(32)  NOT NULL,
    latency_ms      BIGINT,
    result_preview  VARCHAR(2048),
    started_at      TIMESTAMPTZ  NOT NULL,
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_tool_calls_agent_run_id ON tool_calls (agent_run_id, started_at);

CREATE TABLE root_causes (
    id                    UUID PRIMARY KEY,
    incident_id           UUID             NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    statement             TEXT             NOT NULL,
    confidence            DOUBLE PRECISION NOT NULL,
    evidence_json         TEXT             NOT NULL,
    counter_evidence_json TEXT             NOT NULL,
    affected_json         TEXT             NOT NULL,
    created_at            TIMESTAMPTZ      NOT NULL
);

CREATE INDEX idx_root_causes_incident_id ON root_causes (incident_id);
