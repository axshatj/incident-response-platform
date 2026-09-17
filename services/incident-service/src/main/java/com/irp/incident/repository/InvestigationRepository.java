package com.irp.incident.repository;

import com.irp.incident.domain.Investigation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigationRepository extends JpaRepository<Investigation, UUID> {
    List<Investigation> findByIncidentIdOrderByStartedAtDesc(UUID incidentId);
}
