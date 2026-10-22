package com.incidentplatform.application.monitoring;

public final class MonitoredServiceNotFoundException extends RuntimeException {

    public MonitoredServiceNotFoundException(Long serviceId) {
        super("Monitored service " + serviceId + " was not found");
    }
}
