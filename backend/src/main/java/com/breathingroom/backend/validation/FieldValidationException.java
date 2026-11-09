package com.breathingroom.backend.validation;

import java.util.LinkedHashMap;
import java.util.Map;

/** One or more form fields are not acceptable. Each entry is: field name -> a plain-language message for that field. */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("Please fix the highlighted fields.");
        this.errors = new LinkedHashMap<>(errors);
    }

    public Map<String, String> errors() {
        return errors;
    }
}
