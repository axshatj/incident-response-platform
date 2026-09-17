package com.irp.incident.repository;

import com.irp.incident.domain.Observation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {
    List<Observation> findByIncidentIdOrderByObservedAtAsc(UUID incidentId);
}
