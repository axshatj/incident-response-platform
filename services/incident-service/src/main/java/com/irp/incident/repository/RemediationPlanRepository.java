package com.irp.incident.repository;

import com.irp.incident.domain.RemediationPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemediationPlanRepository extends JpaRepository<RemediationPlan, UUID> {

    Optional<RemediationPlan> findByIdempotencyKey(String idempotencyKey);

    List<RemediationPlan> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
