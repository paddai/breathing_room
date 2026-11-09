package com.breathingroom.backend.web;

import com.breathingroom.backend.validation.FieldValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns validation problems into a clear JSON answer the React forms can show next to the right input. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Map<String, Object>> fieldProblems(FieldValidationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", e.getMessage());
        body.put("fieldErrors", e.errors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
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
