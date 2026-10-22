package com.incidentplatform.Entity.exception;

public final class ServiceOutageNotAllowedException extends DomainRuleViolationException {

    public ServiceOutageNotAllowedException(String message) {
        super(message);
    }
}
