package com.incidentplatform.Entity;

import com.google.common.base.Preconditions;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private String serviceUrl;
    private boolean active;
    private HealthStatus status;
    private int consecutiveFailures;
    private int checkIntervalSeconds;

    public Service(String serviceName, String serviceUrl) {
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
        this.active = true;
        this.status = HealthStatus.UNKNOWN;
        this.consecutiveFailures = 0;
        this.checkIntervalSeconds = 30;
    }

    private void checkInvariant() {
        Preconditions.checkNotNull(serviceName, "Service name cannot be null");
        Preconditions.checkState(!serviceName.isEmpty(), "Service name cannot be empty");
        Preconditions.checkNotNull(serviceUrl, "Service url cannot be null");
        Preconditions.checkState(!serviceUrl.isEmpty(), "Service url cannot be empty");
        Preconditions.checkState(consecutiveFailures >=0, "Consecutive failures cannot be negative");
        Preconditions.checkState(checkIntervalSeconds>0, "Check interval period cannot be 0 or less");
    }

    public void changeUrl(String serviceUrl) {
        Preconditions.checkNotNull(serviceUrl, "Service url passed cannot be null");
        Preconditions.checkState(!serviceUrl.isEmpty(), "Service url passed cannot be empty");
        checkInvariant();
        this.serviceUrl = serviceUrl;
        checkInvariant();
    }
    public void recordSuccessfulCheck() {
        checkInvariant();
        consecutiveFailures = 0;
        checkInvariant();
    }
    public void recordFailedCheck() {
        checkInvariant();
        consecutiveFailures ++;
        checkInvariant();
    }
    public void activateService() {
        checkInvariant();
        active = true;
        checkInvariant();
    }
    public void deactivateService() {
        checkInvariant();
        active = false;
        checkInvariant();
    }

    public Long getId() {
        checkInvariant();
        return id;
    }

    public String getServiceName() {
        checkInvariant();
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        Preconditions.checkNotNull(serviceName, "Service name passed cannot be null");
        Preconditions.checkState(!serviceName.isEmpty(), "Service name passed cannot be empty");
        checkInvariant();
        this.serviceName = serviceName;
        checkInvariant();
    }

    public String getServiceUrl() {
        checkInvariant();
        return serviceUrl;
    }


    public boolean isActive() {
        checkInvariant();
        return active;
    }


    public HealthStatus getStatus() {
        checkInvariant();
        return status;
    }

    public void setStatus(HealthStatus status) {
        checkInvariant();
        this.status = status;
        checkInvariant();
    }

    public int getConsecutiveFailures() {
        checkInvariant();
        return consecutiveFailures;
    }


    public int getCheckIntervalSeconds() {
        checkInvariant();
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(int checkIntervalSeconds) {
        Preconditions.checkState(checkIntervalSeconds>0, "Check interval period passed cannot be 0 or less");
        checkInvariant();
        this.checkIntervalSeconds = checkIntervalSeconds;
        checkInvariant();
    }
}
