package com.incidentplatform.Entity.exception;

public final class InvalidIncidentStateTransitionException extends DomainRuleViolationException {

    public InvalidIncidentStateTransitionException(String message) {
        super(message);
    }
}
