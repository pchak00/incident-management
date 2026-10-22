package com.incidentplatform.presentation;

import com.incidentplatform.Entity.exception.DomainRuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleDomainRuleViolation(DomainRuleViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Domain rule violation");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
