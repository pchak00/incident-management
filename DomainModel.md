## Domain Model

### Here is my domain model

```mermaid
classDiagram
    class Service {
        -Long id
        -String serviceName
        -String serviceUrl
        -boolean active
        -HealthStatus status
        -int consecutiveFailures
        -int checkIntervalSeconds
        
        +rename(String serviceName) void
        +changeUrl(String serviceUrl) void
        +changeCheckInterval(int checkIntervalSeconds) void
        +recordSuccessfulCheck() void
        +recordFailedCheck() void
        +activateService() void
        +deactivateService() void
    }
    
    class Engineer {
        -Long id
        -String engineerName
        -String engineerPosition
        -String email
        -Set ~EngineerRole~ roles
        
        +addRole(EngineerRole role) void
        +removeRole(EngineerRole role) void
    }
    
    class Incident {
        -Long id
        -Engineer leadEngineerAssigned
        -Engineer acknowledgedBy
        -Instant createdAt
        -Instant resolvedAt
        -Service serviceAffected
        -String incidentDescription
        -List ~IncidentEvent~ activityTimeline
        -IncidentReport report
        -IncidentStatusType status
        
        +addEvent(IncidentEvent event) void
        +acknowledgeIncident(Engineer engineer) void
        +assignEngineer(Engineer engineer) void
        +recordServiceRecovery() void
        +recordServiceOutage() void
        +attachReport(IncidentReport report) void
        +resolve(Engineer engineer) void
    }
    
    class IncidentReport {
        -Long id
        -Instant createdAt
        -Instant updatedAt
        -String report
        -Incident createdFrom
        -Engineer createdBy
        
        +writeReport(String report)
    }
    
    class IncidentEvent {
        -Long id
        -IncidentEventType eventType
        -Incident incidentFrom
        -Engineer performedBy
        -Instant createdAt
        -String eventDescription
    }
    
    class IncidentEventType {
        <<enumeration>>
        CREATED,
        ACKNOWLEDGED,
        LEAD_ASSIGNED,
        SERVICE_RECOVERED,
        SERVICE_UNHEALTHY,
        RESOLVED
    }
    
    class IncidentStatusType {
        <<enumeration>>
        OPEN,
        ACKNOWLEDGED,
        WORKING,
        RESOLVED
    }
        
    class EngineerRole {
        <<enumeration>>
        RESPONDER,
        ADMIN
    }
    
    class HealthStatus {
        <<enumeration>>
        UNKNOWN,
        HEALTHY,
        FAILING,
        UNHEALTHY
    }
    
    %% Relationships
    Service "1" --> "0..*" Incident : affected service
    Incident "1" *-- "0..1" IncidentReport : optional postmortem
    Incident "1" *-- "1..*" IncidentEvent : activity timeline
    Incident "0..*" --> "0..1" Engineer : acknowledged by
    Incident "0..*" --> "0..1" Engineer : assigned lead
    IncidentEvent "0..*" --> "0..1" Engineer : performed by
    IncidentReport "0..*" --> "1" Engineer : authored by
    EngineerRole -- Engineer
    IncidentStatusType -- Incident
    HealthStatus -- Service
    IncidentEventType -- IncidentEvent
```
