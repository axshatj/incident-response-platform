package com.irp.incident.repository;

import com.irp.incident.domain.ToolCall;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallRepository extends JpaRepository<ToolCall, UUID> {
    List<ToolCall> findByAgentRunIdOrderByStartedAtAsc(UUID agentRunId);
}
