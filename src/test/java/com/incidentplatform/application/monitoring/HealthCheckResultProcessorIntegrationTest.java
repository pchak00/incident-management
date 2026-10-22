package com.incidentplatform.application.monitoring;

import com.incidentplatform.Entity.Engineer;
import com.incidentplatform.Entity.HealthStatus;
import com.incidentplatform.Entity.Incident;
import com.incidentplatform.Entity.IncidentEventType;
import com.incidentplatform.Entity.IncidentStatusType;
import com.incidentplatform.Entity.Service;
import com.incidentplatform.persistence.IncidentRepository;
import com.incidentplatform.persistence.MonitoredServiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HealthCheckResultProcessorIntegrationTest {

    @Autowired
    private HealthCheckResultProcessor processor;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void recordsOnlyGenuineTransitionsAndCreatesANewIncidentAfterResolution() {
        Service service = serviceRepository.save(new Service(
                "Payment API",
                "https://payments.example.com/health"
        ));

        recordFailures(service.getId(), Service.FAILURE_THRESHOLD);
        processor.recordHealthCheckResult(service.getId(), false);

        Incident firstIncident = onlyIncident(service.getId());
        assertEventTypes(firstIncident, IncidentEventType.CREATED);

        processor.recordHealthCheckResult(service.getId(), true);
        processor.recordHealthCheckResult(service.getId(), true);
        recordFailures(service.getId(), Service.FAILURE_THRESHOLD);
        processor.recordHealthCheckResult(service.getId(), false);
        processor.recordHealthCheckResult(service.getId(), true);

        assertEventTypes(
                firstIncident,
                IncidentEventType.CREATED,
                IncidentEventType.SERVICE_RECOVERED,
                IncidentEventType.SERVICE_UNHEALTHY,
                IncidentEventType.SERVICE_RECOVERED
        );

        Engineer engineer = new Engineer("Taylor", "Backend Engineer", "taylor@example.com");
        entityManager.persist(engineer);
        firstIncident.acknowledgeIncident(engineer);
        firstIncident.assignEngineer(engineer);
        firstIncident.resolve(engineer);

        recordFailures(service.getId(), Service.FAILURE_THRESHOLD);

        List<Incident> incidents = incidentRepository
                .findAllByServiceAffectedIdOrderByCreatedAtAscIdAsc(service.getId());
        assertEquals(2, incidents.size());
        assertEquals(IncidentStatusType.RESOLVED, incidents.get(0).getStatus());
        assertEquals(IncidentStatusType.OPEN, incidents.get(1).getStatus());
        assertEventTypes(incidents.get(1), IncidentEventType.CREATED);
    }

    @Test
    void serializesConcurrentResultsToPreventDuplicateIncidentsAndEvents() throws Exception {
        Service service = new Service("Concurrent API", "https://concurrent.example.com/health");
        service.recordFailedCheck();
        service.recordFailedCheck();
        Long serviceId = serviceRepository.saveAndFlush(service).getId();

        try {
            runConcurrently(() -> processor.recordHealthCheckResult(serviceId, false));

            assertEquals(1, incidentRepository.countByServiceAffectedId(serviceId));
            assertEquals(1, countEvents(serviceId, IncidentEventType.CREATED));
            assertEquals(HealthStatus.UNHEALTHY, serviceRepository.findById(serviceId).orElseThrow().getStatus());

            runConcurrently(() -> processor.recordHealthCheckResult(serviceId, true));

            assertEquals(1, incidentRepository.countByServiceAffectedId(serviceId));
            assertEquals(1, countEvents(serviceId, IncidentEventType.SERVICE_RECOVERED));
            assertEquals(HealthStatus.HEALTHY, serviceRepository.findById(serviceId).orElseThrow().getStatus());

            processor.recordHealthCheckResult(serviceId, false);
            processor.recordHealthCheckResult(serviceId, false);
            runConcurrently(() -> processor.recordHealthCheckResult(serviceId, false));

            assertEquals(1, incidentRepository.countByServiceAffectedId(serviceId));
            assertEquals(1, countEvents(serviceId, IncidentEventType.SERVICE_UNHEALTHY));
            assertEquals(HealthStatus.UNHEALTHY, serviceRepository.findById(serviceId).orElseThrow().getStatus());

            runConcurrently(() -> processor.recordHealthCheckResult(serviceId, true));

            assertEquals(2, countEvents(serviceId, IncidentEventType.SERVICE_RECOVERED));
            assertEquals(HealthStatus.HEALTHY, serviceRepository.findById(serviceId).orElseThrow().getStatus());
        } finally {
            deleteMonitoringTestData(serviceId);
        }
    }

    private void recordFailures(Long serviceId, int failures) {
        for (int failure = 0; failure < failures; failure++) {
            processor.recordHealthCheckResult(serviceId, false);
        }
    }

    private Incident onlyIncident(Long serviceId) {
        List<Incident> incidents = incidentRepository
                .findAllByServiceAffectedIdOrderByCreatedAtAscIdAsc(serviceId);
        assertEquals(1, incidents.size());
        return incidents.get(0);
    }

    private void assertEventTypes(Incident incident, IncidentEventType... expectedTypes) {
        assertEquals(
                List.of(expectedTypes),
                incident.getActivityTimeline().stream()
                        .map(event -> event.getEventType())
                        .toList()
        );
    }

    private void runConcurrently(Runnable operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> runWhenReleased(operation, ready, start));
            Future<?> second = executor.submit(() -> runWhenReleased(operation, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void runWhenReleased(Runnable operation, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted", exception);
        }
        operation.run();
    }

    private int countEvents(Long serviceId, IncidentEventType eventType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from incident_events event
                        join incidents incident on incident.id = event.incident_id
                        where incident.service_id = ? and event.event_type = ?
                        """,
                Integer.class,
                serviceId,
                eventType.name()
        );
        return count == null ? 0 : count;
    }

    private void deleteMonitoringTestData(Long serviceId) {
        jdbcTemplate.update(
                "delete from incident_events where incident_id in (select id from incidents where service_id = ?)",
                serviceId
        );
        jdbcTemplate.update(
                "delete from incident_reports where incident_id in (select id from incidents where service_id = ?)",
                serviceId
        );
        jdbcTemplate.update("delete from incidents where service_id = ?", serviceId);
        jdbcTemplate.update("delete from monitored_services where id = ?", serviceId);
    }
}
