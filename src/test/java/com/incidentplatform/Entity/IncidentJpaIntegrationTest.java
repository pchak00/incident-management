package com.incidentplatform.Entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
class IncidentJpaIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsIncidentWithOrderedEvents() {
        Service service = new Service("Payment API", "https://payments.example.com/health");
        Engineer acknowledger = new Engineer("Sarah", "Backend Engineer", "sarah@example.com");
        Engineer lead = new Engineer("Alex", "Backend Engineer", "alex@example.com");
        entityManager.persist(service);
        entityManager.persist(acknowledger);
        entityManager.persist(lead);

        Incident incident = new Incident(service, "Payment health checks failed");
        incident.acknowledgeIncident(acknowledger);
        incident.assignEngineer(lead);
        incident.recordServiceRecovery();
        incident.recordServiceRecovery();
        incident.recordServiceOutage();
        incident.recordServiceOutage();
        incident.recordServiceRecovery();
        incident.resolve(acknowledger);
        entityManager.persist(incident);
        entityManager.flush();

        Long incidentId = incident.getId();
        Long serviceId = service.getId();
        Long acknowledgerId = acknowledger.getId();
        Long leadId = lead.getId();
        entityManager.clear();

        Incident reloaded = entityManager.find(Incident.class, incidentId);

        assertNotNull(reloaded);
        assertEquals(IncidentStatusType.RESOLVED, reloaded.getStatus());
        assertEquals(serviceId, reloaded.getServiceAffected().getId());
        assertEquals(acknowledgerId, reloaded.getAcknowledgedBy().getId());
        assertEquals(leadId, reloaded.getLeadEngineerAssigned().getId());
        assertNotNull(reloaded.getResolvedAt());
        assertNull(reloaded.getReport());
        assertEquals(
                List.of(
                        IncidentEventType.CREATED,
                        IncidentEventType.ACKNOWLEDGED,
                        IncidentEventType.LEAD_ASSIGNED,
                        IncidentEventType.SERVICE_RECOVERED,
                        IncidentEventType.SERVICE_UNHEALTHY,
                        IncidentEventType.SERVICE_RECOVERED,
                        IncidentEventType.RESOLVED
                ),
                reloaded.getActivityTimeline().stream()
                        .map(IncidentEvent::getEventType)
                        .toList()
        );
        reloaded.getActivityTimeline().forEach(event -> {
            assertNotNull(event.getId());
            assertEquals(incidentId, event.getIncidentFrom().getId());
        });
    }

    @Test
    void cascadesAndReloadsOptionalIncidentReport() {
        Service service = new Service("Search API", "https://search.example.com/health");
        Engineer author = new Engineer("Morgan", "Site Reliability Engineer", "morgan@example.com");
        entityManager.persist(service);
        entityManager.persist(author);

        Incident incident = new Incident(service, "Search health checks failed");
        IncidentReport report = new IncidentReport(incident, author, "Initial root cause analysis");
        incident.attachReport(report);
        entityManager.persist(incident);
        entityManager.flush();

        Long incidentId = incident.getId();
        Long authorId = author.getId();
        entityManager.clear();

        Incident reloaded = entityManager.find(Incident.class, incidentId);

        assertNotNull(reloaded);
        assertNotNull(reloaded.getReport());
        assertNotNull(reloaded.getReport().getId());
        assertEquals("Initial root cause analysis", reloaded.getReport().getReport());
        assertEquals(incidentId, reloaded.getReport().getCreatedFrom().getId());
        assertEquals(authorId, reloaded.getReport().getCreatedBy().getId());
        assertEquals(List.of(IncidentEventType.CREATED),
                reloaded.getActivityTimeline().stream()
                        .map(IncidentEvent::getEventType)
                        .toList());
    }
}
