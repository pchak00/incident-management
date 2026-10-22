package com.incidentplatform.Entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    @Test
    void createsActiveServiceWithUnknownHealth() {
        Service service = new Service("Payment API", "https://payments.example.com/health");

        assertEquals("Payment API", service.getServiceName());
        assertEquals("https://payments.example.com/health", service.getServiceUrl());
        assertTrue(service.isActive());
        assertEquals(HealthStatus.UNKNOWN, service.getStatus());
        assertEquals(0, service.getConsecutiveFailures());
        assertEquals(Service.DEFAULT_CHECK_INTERVAL_SECONDS, service.getCheckIntervalSeconds());
    }

    @Test
    void rejectsInvalidRegistrationValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Service(null, "https://payments.example.com/health"));
        assertThrows(IllegalArgumentException.class,
                () -> new Service(" ", "https://payments.example.com/health"));
        assertThrows(IllegalArgumentException.class,
                () -> new Service("Payment API", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Service("Payment API", "ftp://payments.example.com/health"));
        assertThrows(IllegalArgumentException.class,
                () -> new Service("Payment API", "not-a-url"));
        assertThrows(IllegalArgumentException.class,
                () -> new Service("Payment API", "https://payments.example.com/health", 0));
    }

    @Test
    void rejectsInvalidConfigurationChangesWithoutMutatingService() {
        Service service = new Service("Payment API", "https://payments.example.com/health", 60);

        assertThrows(IllegalArgumentException.class, () -> service.rename(" "));
        assertThrows(IllegalArgumentException.class, () -> service.changeUrl("ftp://payments.example.com/health"));
        assertThrows(IllegalArgumentException.class, () -> service.changeCheckInterval(0));

        assertEquals("Payment API", service.getServiceName());
        assertEquals("https://payments.example.com/health", service.getServiceUrl());
        assertEquals(60, service.getCheckIntervalSeconds());
    }

    @Test
    void changesServiceConfigurationThroughDomainMethods() {
        Service service = new Service("Payment API", "https://payments.example.com/health");

        service.rename("Payments API");
        service.changeUrl("https://status.example.com/payments");
        service.changeCheckInterval(90);

        assertEquals("Payments API", service.getServiceName());
        assertEquals("https://status.example.com/payments", service.getServiceUrl());
        assertEquals(90, service.getCheckIntervalSeconds());
    }

    @Test
    void failedChecksFollowConsecutiveFailureInvariant() {
        Service service = new Service("Payment API", "https://payments.example.com/health");

        service.recordFailedCheck();
        assertEquals(HealthStatus.UNKNOWN, service.getStatus());
        assertEquals(1, service.getConsecutiveFailures());

        service.recordFailedCheck();
        assertEquals(HealthStatus.FAILING, service.getStatus());
        assertEquals(2, service.getConsecutiveFailures());

        service.recordFailedCheck();
        assertEquals(HealthStatus.UNHEALTHY, service.getStatus());
        assertEquals(Service.FAILURE_THRESHOLD, service.getConsecutiveFailures());

        service.recordFailedCheck();
        assertEquals(HealthStatus.UNHEALTHY, service.getStatus());
        assertEquals(Service.FAILURE_THRESHOLD + 1, service.getConsecutiveFailures());
    }

    @Test
    void successfulCheckAutomaticallyRestoresHealthyState() {
        Service service = unhealthyService();

        service.recordSuccessfulCheck();

        assertEquals(0, service.getConsecutiveFailures());
        assertEquals(HealthStatus.HEALTHY, service.getStatus());

        service.recordFailedCheck();
        assertEquals(HealthStatus.UNKNOWN, service.getStatus());
        assertEquals(1, service.getConsecutiveFailures());

        service.recordSuccessfulCheck();
        assertEquals(HealthStatus.HEALTHY, service.getStatus());
        assertEquals(0, service.getConsecutiveFailures());
    }

    @Test
    void activationChangesPreserveLastKnownHealth() {
        Service service = new Service("Payment API", "https://payments.example.com/health");
        service.recordFailedCheck();
        service.recordFailedCheck();

        service.deactivateService();

        assertFalse(service.isActive());
        assertEquals(HealthStatus.FAILING, service.getStatus());
        assertEquals(2, service.getConsecutiveFailures());

        service.activateService();

        assertTrue(service.isActive());
        assertEquals(HealthStatus.FAILING, service.getStatus());
        assertEquals(2, service.getConsecutiveFailures());
    }

    private Service unhealthyService() {
        Service service = new Service("Payment API", "https://payments.example.com/health");
        for (int failedCheck = 0; failedCheck < Service.FAILURE_THRESHOLD; failedCheck++) {
            service.recordFailedCheck();
        }
        return service;
    }
}
