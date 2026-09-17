package com.irp.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_runs")
public class AgentRun {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(name = "agent_type", nullable = false, updatable = false)
    private String agentType;

    @Column(name = "model", updatable = false)
    private String model;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AgentRun() {}

    public AgentRun(UUID id,
                    UUID incidentId,
                    String agentType,
                    String model,
                    String status,
                    Instant startedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.agentType = agentType;
        this.model = model;
        this.status = status;
        this.startedAt = startedAt;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public String getAgentType() { return agentType; }
    public String getModel() { return model; }
    public String getStatus() { return status; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Long getLatencyMs() { return latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void complete(String status, Integer inputTokens, Integer outputTokens, long latencyMs, Instant at) {
        this.status = status;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.completedAt = at;
    }

    public void fail(String message, long latencyMs, Instant at) {
        this.status = "FAILED";
        this.errorMessage = message;
        this.latencyMs = latencyMs;
        this.completedAt = at;
    }
}
