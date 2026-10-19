## Domain Model

### Here is my domain model

```mermaid
classDiagram
    class Service {
        
    }
    
    class Engineer {
        
    }
    
    class HealthMonitor {
        
    }
    
    class Incident {
        
    }
    
    class IncidentReport {
        
    }
    
    class IncidentEvent {
        
    }
    
    class IncidentEventType {
        <<enumeration>>
    }
    
    class IncidentStatusType {
        <<enumeration>>
    }
        
    class EngineerRole {
        <<enumeration>>
    }
    
    class HealthStatus {
        <<enumeration>>
    }
    
    %% Relationships
    HealthMonitor --> Service
    Service --> Incident
    Incident --* IncidentReport
    Incident --* IncidentEvent
    Incident --> Engineer
    Engineer <-- IncidentEvent
    Engineer <-- IncidentReport
    EngineerRole -- Engineer
    IncidentStatusType -- Incident
    HealthStatus -- Service
    IncidentEventType -- IncidentEvent
```