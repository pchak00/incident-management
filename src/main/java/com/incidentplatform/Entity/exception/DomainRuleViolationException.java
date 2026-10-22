package com.incidentplatform.Entity.exception;

public abstract class DomainRuleViolationException extends RuntimeException {

    protected DomainRuleViolationException(String message) {
        super(message);
    }
}
