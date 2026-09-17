package com.irp.incident.repository;

import com.irp.incident.domain.RootCause;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RootCauseRepository extends JpaRepository<RootCause, UUID> {
    List<RootCause> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
