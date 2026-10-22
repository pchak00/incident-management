package com.incidentplatform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Entity
@Table(name = "incident_events")
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private IncidentEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, updatable = false)
    private Incident incidentFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_engineer_id", updatable = false)
    private Engineer performedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false, length = 1_000)
    private String eventDescription;

    protected IncidentEvent() {
        // Required by JPA.
    }

    static IncidentEvent created(Incident incident, Instant createdAt) {
        return new IncidentEvent(
                IncidentEventType.CREATED,
                incident,
                null,
                createdAt,
                "Incident created"
        );
    }

    static IncidentEvent acknowledged(Incident incident, Engineer engineer) {
        checkArgument(engineer != null, "Acknowledging engineer cannot be null");
        return new IncidentEvent(
                IncidentEventType.ACKNOWLEDGED,
                incident,
                engineer,
                Instant.now(),
                "Incident acknowledged by " + engineer.getEngineerName()
        );
    }

    static IncidentEvent leadAssigned(Incident incident, Engineer engineer) {
        checkArgument(engineer != null, "Lead engineer cannot be null");
        return new IncidentEvent(
                IncidentEventType.LEAD_ASSIGNED,
                incident,
                engineer,
                Instant.now(),
                engineer.getEngineerName() + " assigned as incident lead"
        );
    }

    static IncidentEvent serviceRecovered(Incident incident) {
        return new IncidentEvent(
                IncidentEventType.SERVICE_RECOVERED,
                incident,
                null,
                Instant.now(),
                "Affected service recovered"
        );
    }

    static IncidentEvent serviceUnhealthy(Incident incident) {
        return new IncidentEvent(
                IncidentEventType.SERVICE_UNHEALTHY,
                incident,
                null,
                Instant.now(),
                "Affected service became unhealthy again"
        );
    }

    static IncidentEvent resolved(Incident incident, Engineer engineer, Instant resolvedAt) {
        checkArgument(engineer != null, "Resolving engineer cannot be null");
        return new IncidentEvent(
                IncidentEventType.RESOLVED,
                incident,
                engineer,
                resolvedAt,
                "Incident resolved by " + engineer.getEngineerName()
        );
    }

    private IncidentEvent(
            IncidentEventType eventType,
            Incident incidentFrom,
            Engineer performedBy,
            Instant createdAt,
            String eventDescription
    ) {
        checkArgument(eventType != null, "Event type cannot be null");
        checkArgument(incidentFrom != null, "Incident cannot be null");
        checkArgument(createdAt != null, "Created time cannot be null");
        validatePerformer(eventType, performedBy);
        String validEventDescription = requireNonBlank(eventDescription, "Event description");

        this.eventType = eventType;
        this.incidentFrom = incidentFrom;
        this.performedBy = performedBy;
        this.createdAt = createdAt;
        this.eventDescription = validEventDescription;
        checkInvariant();
    }

    public Long getId() {
        return id;
    }

    public IncidentEventType getEventType() {
        return eventType;
    }

    public Incident getIncidentFrom() {
        return incidentFrom;
    }

    public Engineer getPerformedBy() {
        return performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    private void checkInvariant() {
        checkState(eventType != null, "Event type cannot be null");
        checkState(incidentFrom != null, "Incident cannot be null");
        checkState(createdAt != null, "Event creation time cannot be null");
        checkState(isNonBlank(eventDescription), "Event description cannot be blank");
        checkState(hasValidPerformer(eventType, performedBy),
                "System events must not have a performer and human events must have one");
    }

    private static void validatePerformer(IncidentEventType eventType, Engineer performedBy) {
        checkArgument(hasValidPerformer(eventType, performedBy),
                "System events must not have a performer and human events must have one");
    }

    private static boolean hasValidPerformer(IncidentEventType eventType, Engineer performedBy) {
        if (eventType == null) {
            return false;
        }
        return switch (eventType) {
            case CREATED, SERVICE_RECOVERED, SERVICE_UNHEALTHY -> performedBy == null;
            case ACKNOWLEDGED, LEAD_ASSIGNED, RESOLVED -> performedBy != null;
        };
    }

    private static String requireNonBlank(String value, String fieldName) {
        checkArgument(isNonBlank(value), "%s cannot be blank", fieldName);
        return value.trim();
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
