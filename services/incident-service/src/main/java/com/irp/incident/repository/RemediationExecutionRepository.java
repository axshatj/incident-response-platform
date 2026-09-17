package com.irp.incident.repository;

import com.irp.incident.domain.RemediationExecution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemediationExecutionRepository extends JpaRepository<RemediationExecution, UUID> {

    Optional<RemediationExecution> findByIdempotencyKey(String idempotencyKey);

    List<RemediationExecution> findByIncidentIdOrderByStartedAtDesc(UUID incidentId);
}
