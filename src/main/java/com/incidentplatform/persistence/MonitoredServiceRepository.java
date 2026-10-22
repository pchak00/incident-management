package com.incidentplatform.persistence;

import com.incidentplatform.Entity.Service;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MonitoredServiceRepository extends JpaRepository<Service, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select monitoredService from Service monitoredService where monitoredService.id = :serviceId")
    Optional<Service> findByIdForUpdate(@Param("serviceId") Long serviceId);
}
