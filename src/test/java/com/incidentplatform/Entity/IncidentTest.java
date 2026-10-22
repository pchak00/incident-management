package com.incidentplatform.Entity;

import com.incidentplatform.Entity.exception.IncidentReportAlreadyAttachedException;
import com.incidentplatform.Entity.exception.InvalidIncidentStateTransitionException;
import com.incidentplatform.Entity.exception.ServiceRecoveryNotAllowedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncidentTest {

    @Test
    void rejectsInvalidConstructionArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new Incident(null, "Payment health checks failed"));
        assertThrows(IllegalArgumentException.class,
                () -> new Incident(service(), " "));
    }

    @Test
    void opensWithCreatedEventAndNoAssignedEngineers() {
        Service service = service();

        Incident incident = new Incident(service, "Payment health checks failed");

        assertEquals(IncidentStatusType.OPEN, incident.getStatus());
        assertSame(service, incident.getServiceAffected());
        assertNotNull(incident.getCreatedAt());
        assertNull(incident.getAcknowledgedBy());
        assertNull(incident.getLeadEngineerAssigned());
        assertNull(incident.getResolvedAt());
        assertEventTypes(incident, IncidentEventType.CREATED);
        assertNull(incident.getActivityTimeline().get(0).getPerformedBy());
    }

    @Test
    void followsAcknowledgedWorkingResolvedLifecycle() {
        Incident incident = incident();
        Engineer sarah = engineer("Sarah", "sarah@example.com");
        Engineer alex = engineer("Alex", "alex@example.com");

        incident.acknowledgeIncident(sarah);
        assertEquals(IncidentStatusType.ACKNOWLEDGED, incident.getStatus());
        assertSame(sarah, incident.getAcknowledgedBy());
        assertNull(incident.getLeadEngineerAssigned());
        assertNull(incident.getResolvedAt());

        incident.assignEngineer(alex);
        assertEquals(IncidentStatusType.WORKING, incident.getStatus());
        assertSame(sarah, incident.getAcknowledgedBy());
        assertSame(alex, incident.getLeadEngineerAssigned());
        assertNull(incident.getResolvedAt());

        incident.resolve(sarah);
        assertEquals(IncidentStatusType.RESOLVED, incident.getStatus());
        assertSame(sarah, incident.getAcknowledgedBy());
        assertSame(alex, incident.getLeadEngineerAssigned());
        assertNotNull(incident.getResolvedAt());
        assertEventTypes(
                incident,
                IncidentEventType.CREATED,
                IncidentEventType.ACKNOWLEDGED,
                IncidentEventType.LEAD_ASSIGNED,
                IncidentEventType.RESOLVED
        );
        assertSame(sarah, incident.getActivityTimeline().get(3).getPerformedBy());
    }

    @Test
    void preventsInvalidLifecycleTransitions() {
        Incident incident = incident();
        Engineer engineer = engineer("Sarah", "sarah@example.com");

        assertThrows(InvalidIncidentStateTransitionException.class,
                () -> incident.assignEngineer(engineer));
        assertThrows(InvalidIncidentStateTransitionException.class,
                () -> incident.resolve(engineer));
        assertEquals(IncidentStatusType.OPEN, incident.getStatus());
        assertEventTypes(incident, IncidentEventType.CREATED);

        incident.acknowledgeIncident(engineer);
        assertThrows(InvalidIncidentStateTransitionException.class,
                () -> incident.acknowledgeIncident(engineer));
        assertEquals(IncidentStatusType.ACKNOWLEDGED, incident.getStatus());
        assertEventTypes(incident, IncidentEventType.CREATED, IncidentEventType.ACKNOWLEDGED);

        incident.assignEngineer(engineer);
        assertThrows(InvalidIncidentStateTransitionException.class,
                () -> incident.assignEngineer(engineer));
        assertEquals(IncidentStatusType.WORKING, incident.getStatus());
        assertEventTypes(
                incident,
                IncidentEventType.CREATED,
                IncidentEventType.ACKNOWLEDGED,
                IncidentEventType.LEAD_ASSIGNED
        );

        incident.resolve(engineer);
        assertThrows(InvalidIncidentStateTransitionException.class,
                () -> incident.resolve(engineer));
        assertThrows(ServiceRecoveryNotAllowedException.class, incident::recordServiceRecovery);
        assertEventTypes(
                incident,
                IncidentEventType.CREATED,
                IncidentEventType.ACKNOWLEDGED,
                IncidentEventType.LEAD_ASSIGNED,
                IncidentEventType.RESOLVED
        );
    }

    @Test
    void duplicateRecoveryNotificationsAreIdempotentAndDoNotResolveIncident() {
        Incident incident = incident();
        Engineer engineer = engineer("Sarah", "sarah@example.com");

        incident.recordServiceRecovery();
        incident.recordServiceRecovery();

        assertEquals(IncidentStatusType.OPEN, incident.getStatus());
        assertNull(incident.getResolvedAt());
        assertEventTypes(incident, IncidentEventType.CREATED, IncidentEventType.SERVICE_RECOVERED);
        assertNull(incident.getActivityTimeline().get(1).getPerformedBy());

        incident.acknowledgeIncident(engineer);
        incident.assignEngineer(engineer);
        incident.resolve(engineer);
        incident.recordServiceRecovery();

        assertEventTypes(
                incident,
                IncidentEventType.CREATED,
                IncidentEventType.SERVICE_RECOVERED,
                IncidentEventType.ACKNOWLEDGED,
                IncidentEventType.LEAD_ASSIGNED,
                IncidentEventType.RESOLVED
        );
    }

    @Test
    void recordsEachRecoveryAndSubsequentOutageTransitionOnce() {
        Incident incident = incident();

        incident.recordServiceOutage();
        incident.recordServiceRecovery();
        incident.recordServiceRecovery();
        incident.recordServiceOutage();
        incident.recordServiceOutage();
        incident.recordServiceRecovery();
        incident.recordServiceRecovery();

        assertEquals(IncidentStatusType.OPEN, incident.getStatus());
        assertEventTypes(
                incident,
                IncidentEventType.CREATED,
                IncidentEventType.SERVICE_RECOVERED,
                IncidentEventType.SERVICE_UNHEALTHY,
                IncidentEventType.SERVICE_RECOVERED
        );
    }

    @Test
    void resolvesManuallyEvenWhenAffectedServiceRemainsUnhealthy() {
        Service service = service();
        for (int failedCheck = 0; failedCheck < Service.FAILURE_THRESHOLD; failedCheck++) {
            service.recordFailedCheck();
        }
        Incident incident = new Incident(service, "Payment health checks failed");
        Engineer engineer = engineer("Sarah", "sarah@example.com");

        incident.acknowledgeIncident(engineer);
        incident.assignEngineer(engineer);
        incident.resolve(engineer);

        assertEquals(HealthStatus.UNHEALTHY, service.getStatus());
        assertEquals(IncidentStatusType.RESOLVED, incident.getStatus());
        assertNotNull(incident.getResolvedAt());
    }

    @Test
    void timelineCannotBeModifiedOutsideIncident() {
        Incident incident = incident();

        assertThrows(UnsupportedOperationException.class,
                () -> incident.getActivityTimeline().clear());
        assertFalse(incident.getActivityTimeline().isEmpty());
    }

    @Test
    void acceptsOnlyOneReportBelongingToTheIncident() {
        Incident incident = incident();
        Incident anotherIncident = new Incident(service(), "Another outage");
        Engineer author = engineer("Alex", "alex@example.com");
        IncidentReport report = new IncidentReport(incident, author, "Root cause details");

        incident.attachReport(report);

        assertSame(report, incident.getReport());
        assertThrows(IncidentReportAlreadyAttachedException.class,
                () -> incident.attachReport(new IncidentReport(incident, author, "Second report")));
        assertThrows(IllegalArgumentException.class,
                () -> anotherIncident.attachReport(report));
        assertNull(anotherIncident.getReport());
    }

    private void assertEventTypes(Incident incident, IncidentEventType... expectedTypes) {
        List<IncidentEventType> actualTypes = incident.getActivityTimeline().stream()
                .map(IncidentEvent::getEventType)
                .toList();
        assertEquals(List.of(expectedTypes), actualTypes);
    }

    private Incident incident() {
        return new Incident(service(), "Payment health checks failed");
    }

    private Service service() {
        return new Service("Payment API", "https://payments.example.com/health");
    }

    private Engineer engineer(String name, String email) {
        return new Engineer(name, "Backend Engineer", email);
    }
}
