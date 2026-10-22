package com.incidentplatform.Entity;

import com.incidentplatform.Entity.exception.IncidentReportAlreadyAttachedException;
import com.incidentplatform.Entity.exception.InvalidIncidentStateTransitionException;
import com.incidentplatform.Entity.exception.ServiceOutageNotAllowedException;
import com.incidentplatform.Entity.exception.ServiceRecoveryNotAllowedException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_engineer_id")
    private Engineer leadEngineerAssigned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by_engineer_id")
    private Engineer acknowledgedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private Service serviceAffected;

    @Column(nullable = false, length = 2_000)
    private String incidentDescription;

    @OneToMany(
            mappedBy = "incidentFrom",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false
    )
    @OrderBy("createdAt ASC, id ASC")
    private List<IncidentEvent> activityTimeline = new ArrayList<>();

    @OneToOne(
            mappedBy = "createdFrom",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private IncidentReport report;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatusType status;

    protected Incident() {
        // Required by JPA.
    }

    public Incident(Service serviceAffected, String incidentDescription) {
        checkArgument(serviceAffected != null, "Affected service cannot be null");
        String validIncidentDescription = requireNonBlank(incidentDescription, "Incident description");

        this.serviceAffected = serviceAffected;
        this.incidentDescription = validIncidentDescription;
        this.createdAt = Instant.now();
        this.status = IncidentStatusType.OPEN;
        addEvent(IncidentEvent.created(this, createdAt));
    }

    public void acknowledgeIncident(Engineer engineer) {
        checkArgument(engineer != null, "Acknowledging engineer cannot be null");
        requireStatus(IncidentStatusType.OPEN, "Only an open incident can be acknowledged");

        IncidentEvent event = IncidentEvent.acknowledged(this, engineer);
        acknowledgedBy = engineer;
        status = IncidentStatusType.ACKNOWLEDGED;
        addEvent(event);
    }

    public void assignEngineer(Engineer engineer) {
        checkArgument(engineer != null, "Lead engineer cannot be null");
        requireStatus(IncidentStatusType.ACKNOWLEDGED, "Only an acknowledged incident can receive a lead");

        IncidentEvent event = IncidentEvent.leadAssigned(this, engineer);
        leadEngineerAssigned = engineer;
        status = IncidentStatusType.WORKING;
        addEvent(event);
    }

    public void recordServiceRecovery() {
        if (lastServiceHealthEventType() == IncidentEventType.SERVICE_RECOVERED) {
            return;
        }
        if (status == IncidentStatusType.RESOLVED) {
            throw new ServiceRecoveryNotAllowedException("A resolved incident cannot record recovery");
        }

        addEvent(IncidentEvent.serviceRecovered(this));
    }

    public void recordServiceOutage() {
        if (lastServiceHealthEventType() != IncidentEventType.SERVICE_RECOVERED) {
            return;
        }
        if (status == IncidentStatusType.RESOLVED) {
            throw new ServiceOutageNotAllowedException("A resolved incident cannot record a new outage");
        }

        addEvent(IncidentEvent.serviceUnhealthy(this));
    }

    public void attachReport(IncidentReport report) {
        checkArgument(report != null, "Incident report cannot be null");
        if (this.report != null) {
            throw new IncidentReportAlreadyAttachedException();
        }
        checkArgument(report.getCreatedFrom() == this, "Report belongs to a different incident");

        this.report = report;
        checkInvariant();
    }

    public void resolve(Engineer engineer) {
        checkArgument(engineer != null, "Resolving engineer cannot be null");
        requireStatus(IncidentStatusType.WORKING, "Only a working incident can be resolved");
        checkState(leadEngineerAssigned != null, "A working incident must have an assigned lead");

        Instant resolutionTime = Instant.now();
        IncidentEvent event = IncidentEvent.resolved(this, engineer, resolutionTime);
        resolvedAt = resolutionTime;
        status = IncidentStatusType.RESOLVED;
        addEvent(event);
    }

    public Long getId() {
        return id;
    }

    public Engineer getLeadEngineerAssigned() {
        return leadEngineerAssigned;
    }

    public Engineer getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Service getServiceAffected() {
        return serviceAffected;
    }

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public List<IncidentEvent> getActivityTimeline() {
        return Collections.unmodifiableList(activityTimeline);
    }

    public IncidentReport getReport() {
        return report;
    }

    public IncidentStatusType getStatus() {
        return status;
    }

    private void addEvent(IncidentEvent event) {
        checkArgument(event != null, "Incident event cannot be null");
        checkArgument(event.getIncidentFrom() == this, "Event belongs to a different incident");

        activityTimeline.add(event);
        checkInvariant();
    }

    private void requireStatus(IncidentStatusType requiredStatus, String message) {
        if (status != requiredStatus) {
            throw new InvalidIncidentStateTransitionException(message);
        }
    }

    private IncidentEventType lastServiceHealthEventType() {
        for (int eventIndex = activityTimeline.size() - 1; eventIndex >= 0; eventIndex--) {
            IncidentEventType eventType = activityTimeline.get(eventIndex).getEventType();
            if (eventType == IncidentEventType.SERVICE_RECOVERED
                    || eventType == IncidentEventType.SERVICE_UNHEALTHY) {
                return eventType;
            }
        }
        return null;
    }

    private void checkInvariant() {
        checkState(serviceAffected != null, "An incident must reference an affected service");
        checkState(isNonBlank(incidentDescription), "Incident description cannot be blank");
        checkState(createdAt != null, "Incident creation time cannot be null");
        checkState(status != null, "Incident status cannot be null");
        checkState(activityTimeline != null, "Incident timeline cannot be null");
        checkState(!activityTimeline.isEmpty(), "Incident timeline must contain a created event");
        checkState(activityTimeline.get(0) != null, "The first incident event cannot be null");
        checkState(activityTimeline.get(0).getEventType() == IncidentEventType.CREATED,
                "The first incident event must be the created event");
        checkState(activityTimeline.get(0).getIncidentFrom() == this,
                "The created event must belong to this incident");
        checkState(report == null || report.getCreatedFrom() == this,
                "Attached report must belong to this incident");

        switch (status) {
            case OPEN -> {
                checkState(acknowledgedBy == null, "Open incidents cannot have an acknowledger");
                checkState(leadEngineerAssigned == null, "Open incidents cannot have a lead");
                checkState(resolvedAt == null, "Open incidents cannot have a resolution time");
            }
            case ACKNOWLEDGED -> {
                checkState(acknowledgedBy != null, "Acknowledged incidents must have an acknowledger");
                checkState(leadEngineerAssigned == null, "Acknowledged incidents cannot have a lead yet");
                checkState(resolvedAt == null, "Acknowledged incidents cannot have a resolution time");
            }
            case WORKING -> {
                checkState(acknowledgedBy != null, "Working incidents must have an acknowledger");
                checkState(leadEngineerAssigned != null, "Working incidents must have a lead");
                checkState(resolvedAt == null, "Working incidents cannot have a resolution time");
            }
            case RESOLVED -> {
                checkState(acknowledgedBy != null, "Resolved incidents must preserve the acknowledger");
                checkState(leadEngineerAssigned != null, "Resolved incidents must preserve the lead");
                checkState(resolvedAt != null, "Resolved incidents must have a resolution time");
                checkState(!resolvedAt.isBefore(createdAt),
                        "Incident resolution time cannot precede its creation time");
            }
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        checkArgument(isNonBlank(value), "%s cannot be blank", fieldName);
        return value.trim();
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
