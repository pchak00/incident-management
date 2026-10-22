package com.incidentplatform.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Lob
    @Column(nullable = false)
    private String report;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true, updatable = false)
    private Incident createdFrom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_engineer_id", nullable = false, updatable = false)
    private Engineer createdBy;

    protected IncidentReport() {
    }

    public IncidentReport(Incident createdFrom, Engineer createdBy, String report) {
        checkArgument(createdFrom != null, "Incident cannot be null");
        checkArgument(createdBy != null, "Report author cannot be null");
        String validReport = requireNonBlank(report, "Report content");

        this.createdFrom = createdFrom;
        this.createdBy = createdBy;
        this.report = validReport;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        checkInvariant();
    }

    public void writeReport(String report) {
        checkInvariant();
        String validReport = requireNonBlank(report, "Report content");
        Instant updateTime = Instant.now();

        this.report = validReport;
        this.updatedAt = updateTime;
        checkInvariant();
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getReport() {
        return report;
    }

    public Incident getCreatedFrom() {
        return createdFrom;
    }

    public Engineer getCreatedBy() {
        return createdBy;
    }

    private void checkInvariant() {
        checkState(createdFrom != null, "Report must belong to an incident");
        checkState(createdBy != null, "Report must preserve its original author");
        checkState(isNonBlank(report), "Report content cannot be blank");
        checkState(createdAt != null, "Report creation time cannot be null");
        checkState(updatedAt != null, "Report update time cannot be null");
        checkState(!updatedAt.isBefore(createdAt), "Report update time cannot precede its creation time");
    }

    private static String requireNonBlank(String value, String fieldName) {
        checkArgument(isNonBlank(value), "%s cannot be blank", fieldName);
        return value.trim();
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
