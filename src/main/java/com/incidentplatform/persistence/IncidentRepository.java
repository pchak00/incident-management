package com.incidentplatform.persistence;

import com.incidentplatform.Entity.Incident;
import com.incidentplatform.Entity.IncidentStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select incident
            from Incident incident
            where incident.serviceAffected.id = :serviceId
              and incident.status <> :resolvedStatus
            order by incident.createdAt desc, incident.id desc
            """)
    List<Incident> findUnresolvedByServiceIdForUpdate(
            @Param("serviceId") Long serviceId,
            @Param("resolvedStatus") IncidentStatusType resolvedStatus
    );

    List<Incident> findAllByServiceAffectedIdOrderByCreatedAtAscIdAsc(Long serviceId);

    long countByServiceAffectedId(Long serviceId);
}
