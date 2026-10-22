package com.incidentplatform.Entity.exception;

public final class ServiceRecoveryNotAllowedException extends DomainRuleViolationException {

    public ServiceRecoveryNotAllowedException(String message) {
        super(message);
    }
}
