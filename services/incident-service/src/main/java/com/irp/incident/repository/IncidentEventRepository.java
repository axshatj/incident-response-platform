package com.irp.incident.repository;

import com.irp.incident.domain.IncidentEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, UUID> {

    List<IncidentEvent> findByIncidentIdOrderByOccurredAtAsc(UUID incidentId);
}
