package com.breathingroom.backend.validation;

import java.util.LinkedHashMap;
import java.util.Map;

/** One or more form fields are not acceptable. Each entry is: field name -> a plain-language message for that field. */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> errors;
    private final int status;

    /** Ordinary validation problem: answered with HTTP 400. */
    public FieldValidationException(Map<String, String> errors) {
        this(400, errors);
    }

    /** Same, with a specific HTTP status (for example 409 when the email is already taken). */
    public FieldValidationException(int status, Map<String, String> errors) {
        super("Please fix the highlighted fields.");
        this.status = status;
        this.errors = new LinkedHashMap<>(errors);
    }

    public Map<String, String> errors() {
        return errors;
    }

    public int status() {
        return status;
    }
}
