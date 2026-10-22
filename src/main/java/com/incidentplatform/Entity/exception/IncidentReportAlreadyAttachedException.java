package com.incidentplatform.Entity.exception;

public final class IncidentReportAlreadyAttachedException extends DomainRuleViolationException {

    public IncidentReportAlreadyAttachedException() {
        super("An incident can have only one report");
    }
}
