package com.irp.incident.repository;

import com.irp.incident.domain.Incident;
import com.irp.incident.domain.IncidentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findAllByOrderByDetectedAtDesc();

    List<Incident> findByStatusOrderByDetectedAtDesc(IncidentStatus status);
}
