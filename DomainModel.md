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
    HealthMonitor "1"-->"0..*" Service
    Service "1"-->"0..*" Incident
    Incident "1"--*"0..1" IncidentReport
    Incident "1"--*"1..*" IncidentEvent
    Incident "1"-->"1" Engineer
    Engineer "1"<--"1" IncidentEvent
    Engineer "1"<--"1" IncidentReport
    EngineerRole -- Engineer
    IncidentStatusType -- Incident
    HealthStatus -- Service
    IncidentEventType -- IncidentEvent
```