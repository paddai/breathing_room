package com.breathingroom.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.breathingroom.backend.logic.AuthMessages;
import com.breathingroom.backend.validation.FieldValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void duplicateEmailIsAnEmailFieldMessageWithStatus409() {
        var e = new FieldValidationException(409, Map.of("email", AuthMessages.EMAIL_TAKEN));
        ResponseEntity<Map<String, Object>> res = handler.fieldProblems(e);
        assertEquals(409, res.getStatusCode().value());
        assertEquals(409, res.getBody().get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) res.getBody().get("fieldErrors");
        assertEquals("An account with this email already exists. Please sign in or use a different email.", fields.get("email"));
    }

    @Test
    void ordinaryFieldProblemsStayAt400() {
        ResponseEntity<Map<String, Object>> res = handler.fieldProblems(new FieldValidationException(Map.of("amount", "Enter a number.")));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void reasonFromResponseStatusExceptionReachesTheMessage() {
        ResponseEntity<Map<String, Object>> res = handler.withReason(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        assertEquals(401, res.getStatusCode().value());
        assertEquals("Invalid email or password.", res.getBody().get("message"));
    }
}
