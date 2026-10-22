package com.incidentplatform.application.monitoring;

import com.incidentplatform.Entity.HealthStatus;
import com.incidentplatform.Entity.Incident;
import com.incidentplatform.Entity.IncidentStatusType;
import com.incidentplatform.Entity.Service;
import com.incidentplatform.persistence.IncidentRepository;
import com.incidentplatform.persistence.MonitoredServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@org.springframework.stereotype.Service
public class HealthCheckResultProcessor {

    private final MonitoredServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;

    public HealthCheckResultProcessor(
            MonitoredServiceRepository serviceRepository,
            IncidentRepository incidentRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void recordHealthCheckResult(Long serviceId, boolean successful) {
        checkArgument(serviceId != null, "Service id cannot be null");

        Service service = serviceRepository.findByIdForUpdate(serviceId)
                .orElseThrow(() -> new MonitoredServiceNotFoundException(serviceId));
        HealthStatus previousStatus = service.getStatus();

        if (successful) {
            service.recordSuccessfulCheck();
        } else {
            service.recordFailedCheck();
        }

        HealthStatus currentStatus = service.getStatus();
        if (previousStatus == HealthStatus.UNHEALTHY && currentStatus == HealthStatus.HEALTHY) {
            unresolvedIncident(serviceId).ifPresent(Incident::recordServiceRecovery);
        } else if (previousStatus != HealthStatus.UNHEALTHY && currentStatus == HealthStatus.UNHEALTHY) {
            recordOutageTransition(service);
        }
    }

    private void recordOutageTransition(Service service) {
        Optional<Incident> unresolvedIncident = unresolvedIncident(service.getId());
        if (unresolvedIncident.isPresent()) {
            unresolvedIncident.get().recordServiceOutage();
            return;
        }

        incidentRepository.save(new Incident(
                service,
                service.getServiceName() + " failed consecutive health checks"
        ));
    }

    private Optional<Incident> unresolvedIncident(Long serviceId) {
        List<Incident> unresolvedIncidents = incidentRepository.findUnresolvedByServiceIdForUpdate(
                serviceId,
                IncidentStatusType.RESOLVED
        );
        checkState(unresolvedIncidents.size() <= 1,
                "A service cannot have more than one unresolved incident");
        return unresolvedIncidents.stream().findFirst();
    }
}
