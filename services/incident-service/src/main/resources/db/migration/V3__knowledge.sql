-- Phase 5: knowledge base. Requires the pgvector image (pgvector/pgvector:pg16).

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_documents (
    id           UUID PRIMARY KEY,
    source       VARCHAR(64)  NOT NULL,
    path         VARCHAR(256) NOT NULL UNIQUE,
    title        VARCHAR(256) NOT NULL,
    service      VARCHAR(128),
    team         VARCHAR(64),
    environment  VARCHAR(64),
    severity     VARCHAR(16),
    tags         VARCHAR(256),
    content      TEXT         NOT NULL,
    embedding    vector(384)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_knowledge_service ON knowledge_documents (service);
CREATE INDEX idx_knowledge_source  ON knowledge_documents (source);
