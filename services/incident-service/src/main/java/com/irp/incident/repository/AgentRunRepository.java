package com.irp.incident.repository;

import com.irp.incident.domain.AgentRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
    List<AgentRun> findByIncidentIdOrderByStartedAtAsc(UUID incidentId);
}
