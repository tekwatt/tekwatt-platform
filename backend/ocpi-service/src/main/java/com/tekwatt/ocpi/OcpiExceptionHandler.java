package com.tekwatt.ocpi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        OcpiModulesController.class, OcpiCredentialsController.class, OcpiCommandsController.class
})
public class OcpiExceptionHandler {
    @ExceptionHandler(OcpiController.MissingLocation.class)
    ResponseEntity<Map<String, Object>> missing() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(OcpiProtocol.error(2003, "Unknown OCPI object"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(OcpiProtocol.error(2001, exception.getMessage() == null ? "Invalid OCPI request" : exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(OcpiProtocol.error(
                exception.getStatusCode().value() == 401 ? 2000
                        : exception.getStatusCode().value() == 502 ? 3001 : 3000,
                exception.getReason() == null ? "OCPI request could not be completed" : exception.getReason()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OcpiProtocol.error(3000, "OCPI data is temporarily unavailable"));
    }
}
