package com.breathingroom.backend.logic;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Simple, length-first password and email rules (current guidance favors length over complex symbol rules). */
public final class PasswordPolicy {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> COMMON = Set.of("password", "password1", "password12", "password123", "1234567890", "12345678910",
            "qwertyuiop", "qwerty12345", "letmein123", "iloveyou123", "admin12345", "welcome123", "changeme123", "passw0rd123");

    private PasswordPolicy() {}

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static boolean validEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    /** @return an error message the user can act on, or empty if the password is acceptable. */
    public static Optional<String> validate(String password, String email) {
        if (password == null || password.isEmpty()) return Optional.of("Enter a password.");
        if (password.length() < 10) return Optional.of("Use at least 10 characters.");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) return Optional.of("Use 72 characters or fewer.");
        if (COMMON.contains(password.toLowerCase())) return Optional.of("That password is too common. Try a short phrase only you would know.");
        if (password.chars().distinct().count() < 5) return Optional.of("Use a wider mix of characters.");
        String name = normalizeEmail(email);
        int at = name.indexOf('@');
        if (at >= 4 && password.toLowerCase().contains(name.substring(0, at))) return Optional.of("Do not put your email name inside your password.");
        return Optional.empty();
    }
}
