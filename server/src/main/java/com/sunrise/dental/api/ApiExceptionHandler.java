package com.sunrise.dental.api;

import com.sunrise.dental.exception.BillAlreadyIssuedException;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.NotFoundException;
import com.sunrise.dental.exception.OutsideOpeningHoursException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into HTTP responses for the API channel.
 *
 * <p>Without this, a rejected booking would surface as a 500 and the console client could not
 * tell a genuine server fault from a slot that was simply already taken. Each rule maps to the
 * status that describes it: 404 for a missing appointment, 409 for a conflict with existing
 * data, 400 for input the caller can correct.</p>
 */
@RestControllerAdvice(basePackages = "com.sunrise.dental.api")
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({DoubleBookingException.class, BillAlreadyIssuedException.class})
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OutsideOpeningHoursException.class)
    public ResponseEntity<Map<String, Object>> badRequest(OutsideOpeningHoursException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Field-level validation failures, returned per field so the caller can show them inline. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(baseBody(status, message));
    }

    private Map<String, Object> baseBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
