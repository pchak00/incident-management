package com.incidentplatform.Entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncidentReportTest {

    @Test
    void preservesOriginalIncidentAndAuthorWhenEdited() {
        Incident incident = new Incident(
                new Service("Payment API", "https://payments.example.com/health"),
                "Payment health checks failed"
        );
        Engineer author = new Engineer("Alex", "Backend Engineer", "alex@example.com");
        IncidentReport report = new IncidentReport(incident, author, "Initial findings");
        Instant createdAt = report.getCreatedAt();

        report.writeReport("Completed postmortem");

        assertEquals("Completed postmortem", report.getReport());
        assertEquals(createdAt, report.getCreatedAt());
        assertFalse(report.getUpdatedAt().isBefore(createdAt));
        assertSame(incident, report.getCreatedFrom());
        assertSame(author, report.getCreatedBy());
    }

    @Test
    void rejectsBlankReportContent() {
        Incident incident = new Incident(
                new Service("Payment API", "https://payments.example.com/health"),
                "Payment health checks failed"
        );
        Engineer author = new Engineer("Alex", "Backend Engineer", "alex@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> new IncidentReport(incident, author, " "));

        IncidentReport report = new IncidentReport(incident, author, "Initial findings");
        Instant updatedAt = report.getUpdatedAt();
        assertThrows(IllegalArgumentException.class, () -> report.writeReport(" "));
        assertEquals("Initial findings", report.getReport());
        assertEquals(updatedAt, report.getUpdatedAt());
    }
}
