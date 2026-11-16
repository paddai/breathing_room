package com.breathingroom.backend.web;

import com.breathingroom.backend.validation.FieldValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Turns validation problems into a clear JSON answer the React forms can show next to the right input. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Map<String, Object>> fieldProblems(FieldValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null) status = HttpStatus.BAD_REQUEST;
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", e.getMessage());
        body.put("fieldErrors", e.errors());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Spring leaves the reason out of the JSON body of a ResponseStatusException, so the browser used to see only a bare status
     * number (for example "error 409"). This puts the plain-language reason into "message" so the screens can show it.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> withReason(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", e.getStatusCode().value());
        body.put("error", status == null ? "Error" : status.getReasonPhrase());
        body.put("message", e.getReason() == null ? "Something went wrong. Please try again." : e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    /** For example text typed into a number box that got past the browser. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "Some of the values are not in the right format. Check the numbers and dates and try again.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
