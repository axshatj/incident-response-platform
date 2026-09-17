package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_calls")
public class ToolCall {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agent_run_id", nullable = false, updatable = false)
    private UUID agentRunId;

    @Column(name = "tool_name", nullable = false, updatable = false)
    private String toolName;

    @Column(name = "arguments_hash", updatable = false)
    private String argumentsHash;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "result_preview", updatable = false)
    private String resultPreview;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ToolCall() {}

    public ToolCall(UUID id,
                    UUID agentRunId,
                    String toolName,
                    String argumentsHash,
                    String status,
                    Long latencyMs,
                    String resultPreview,
                    Instant startedAt,
                    Instant completedAt) {
        this.id = id;
        this.agentRunId = agentRunId;
        this.toolName = toolName;
        this.argumentsHash = argumentsHash;
        this.status = status;
        this.latencyMs = latencyMs;
        this.resultPreview = resultPreview;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getAgentRunId() { return agentRunId; }
    public String getToolName() { return toolName; }
    public String getArgumentsHash() { return argumentsHash; }
    public String getStatus() { return status; }
    public Long getLatencyMs() { return latencyMs; }
    public String getResultPreview() { return resultPreview; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
