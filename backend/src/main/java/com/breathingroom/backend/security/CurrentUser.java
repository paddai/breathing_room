package com.breathingroom.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** The id of the signed-in user. Controllers call this and pass it to every query, so data is never shared between users. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) return id;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
    }
}
