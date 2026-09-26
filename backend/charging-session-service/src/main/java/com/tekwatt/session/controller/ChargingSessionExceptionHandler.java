package com.tekwatt.session.controller;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = ChargingSessionController.class)
public class ChargingSessionExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> status(ResponseStatusException exception) {
        String detail = Optional.ofNullable(exception.getReason()).orElse("Charging session request could not be completed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), detail);
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null) problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(exception.getStatusCode()).body(problem);
    }
}
