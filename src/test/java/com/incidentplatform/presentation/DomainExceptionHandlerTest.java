package com.incidentplatform.presentation;

import com.incidentplatform.Entity.exception.InvalidIncidentStateTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainExceptionHandlerTest {

    @Test
    void translatesExpectedDomainViolationToConflictProblemDetail() {
        DomainExceptionHandler handler = new DomainExceptionHandler();
        InvalidIncidentStateTransitionException exception =
                new InvalidIncidentStateTransitionException("Only an open incident can be acknowledged");

        ResponseEntity<ProblemDetail> response = handler.handleDomainRuleViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals("Domain rule violation", response.getBody().getTitle());
        assertEquals(exception.getMessage(), response.getBody().getDetail());
    }
}
