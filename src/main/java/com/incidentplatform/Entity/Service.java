package com.incidentplatform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.net.URI;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Entity
@Table(name = "monitored_services")
public class Service {

    public static final int DEFAULT_CHECK_INTERVAL_SECONDS = 30;
    public static final int FAILURE_THRESHOLD = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String serviceUrl;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthStatus status;

    @Column(nullable = false)
    private int consecutiveFailures;

    @Column(nullable = false)
    private int checkIntervalSeconds;

    protected Service() {
    }

    public Service(String serviceName, String serviceUrl) {
        this(serviceName, serviceUrl, DEFAULT_CHECK_INTERVAL_SECONDS);
    }

    public Service(String serviceName, String serviceUrl, int checkIntervalSeconds) {
        String validServiceName = requireNonBlank(serviceName, "Service name");
        String validServiceUrl = requireValidHealthCheckUrl(serviceUrl);
        checkArgument(checkIntervalSeconds > 0, "Check interval must be greater than zero");

        this.serviceName = validServiceName;
        this.serviceUrl = validServiceUrl;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.active = true;
        this.status = HealthStatus.UNKNOWN;
        this.consecutiveFailures = 0;
        checkInvariant();
    }

    public void changeUrl(String serviceUrl) {
        String validServiceUrl = requireValidHealthCheckUrl(serviceUrl);

        this.serviceUrl = validServiceUrl;
        checkInvariant();
    }

    public void recordSuccessfulCheck() {
        consecutiveFailures = 0;
        status = HealthStatus.HEALTHY;

        checkInvariant();
    }

    public void recordFailedCheck() {
        checkState(consecutiveFailures < Integer.MAX_VALUE, "Consecutive failure count overflow");

        consecutiveFailures++;
        status = switch (consecutiveFailures) {
            case 1 -> HealthStatus.UNKNOWN;
            case 2 -> HealthStatus.FAILING;
            default -> HealthStatus.UNHEALTHY;
        };
        checkInvariant();
    }

    public void activateService() {
        active = true;
        checkInvariant();
    }

    public void deactivateService() {
        active = false;
        checkInvariant();
    }

    public Long getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void rename(String serviceName) {
        String validServiceName = requireNonBlank(serviceName, "Service name");

        this.serviceName = validServiceName;
        checkInvariant();
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public boolean isActive() {
        return active;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void changeCheckInterval(int checkIntervalSeconds) {
        checkArgument(checkIntervalSeconds > 0, "Check interval must be greater than zero");

        this.checkIntervalSeconds = checkIntervalSeconds;
        checkInvariant();
    }

    private void checkInvariant() {
        checkState(isNonBlank(serviceName), "Service name cannot be blank");
        checkState(isValidHealthCheckUrl(serviceUrl), "Service URL must be a valid absolute HTTP or HTTPS URL");
        checkState(status != null, "Health status cannot be null");
        checkState(consecutiveFailures >= 0, "Consecutive failures cannot be negative");
        checkState(checkIntervalSeconds > 0, "Check interval must be greater than zero");

        switch (status) {
            case UNKNOWN -> checkState(consecutiveFailures <= 1,
                    "Unknown services can have at most one consecutive failure");
            case HEALTHY -> checkState(consecutiveFailures == 0,
                    "Healthy services cannot have consecutive failures");
            case FAILING -> checkState(consecutiveFailures == 2,
                    "Failing services must have exactly two consecutive failures");
            case UNHEALTHY -> checkState(consecutiveFailures >= FAILURE_THRESHOLD,
                    "Unhealthy services must have reached the failure threshold");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        checkArgument(isNonBlank(value), "%s cannot be blank", fieldName);
        return value.trim();
    }

    private static String requireValidHealthCheckUrl(String serviceUrl) {
        String value = requireNonBlank(serviceUrl, "Service URL");
        checkArgument(isValidHealthCheckUrl(value),
                "Service URL must be a valid absolute HTTP or HTTPS URL");
        return value;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isValidHealthCheckUrl(String value) {
        if (!isNonBlank(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            return (normalizedScheme.equals("http") || normalizedScheme.equals("https"))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
