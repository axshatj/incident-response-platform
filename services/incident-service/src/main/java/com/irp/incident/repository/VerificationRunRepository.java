package com.irp.incident.repository;

import com.irp.incident.domain.VerificationRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRunRepository extends JpaRepository<VerificationRun, UUID> {

    List<VerificationRun> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
