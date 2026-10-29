package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.LoginThrottle;
import com.breathingroom.backend.logic.PasswordPolicy;
import com.breathingroom.backend.logic.TokenService;
import com.breathingroom.backend.model.AppUser;
import com.breathingroom.backend.repository.AppUserRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Sign up, sign in, and delete account. Passwords are never stored or logged, only BCrypt hashes. */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginThrottle throttle;
    private final DemoDataService data;
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, TokenService tokens, LoginThrottle throttle, DemoDataService data) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.throttle = throttle;
        this.data = data;
        this.dummyHash = encoder.encode("timing-protection-only"); // so unknown emails take as long as real ones
    }

    private Dtos.AuthResponse respond(AppUser u) {
        return new Dtos.AuthResponse(tokens.issue(u.getId()), tokens.ttlSeconds(), new Dtos.UserDto(u.getId(), u.getEmail()));
    }

    @Transactional
    public Dtos.AuthResponse register(Dtos.AuthRequest req) {
        String email = PasswordPolicy.normalizeEmail(req == null ? null : req.email());
        if (!PasswordPolicy.validEmail(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        Optional<String> problem = PasswordPolicy.validate(req.password(), email);
        if (problem.isPresent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem.get());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists. Try signing in.");

        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        return respond(users.save(u));
    }

    public Dtos.AuthResponse login(Dtos.AuthRequest req) {
        String email = PasswordPolicy.normalizeEmail(req == null ? null : req.email());
        String password = req == null || req.password() == null ? "" : req.password();
        if (throttle.isLocked(email)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many tries. Please wait about 10 minutes and try again.");
        }
        Optional<AppUser> found = users.findByEmail(email);
        boolean ok = encoder.matches(password, found.map(AppUser::getPasswordHash).orElse(dummyHash)) && found.isPresent();
        if (!ok) {
            throttle.recordFailure(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        throttle.reset(email);
        return respond(found.get());
    }

    public Dtos.UserDto me(Long userId) {
        AppUser u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
        return new Dtos.UserDto(u.getId(), u.getEmail());
    }

    /** Permanently deletes the account and every row it owns. Requires the password again. */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        AppUser u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
        if (password == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "That password is not correct.");
        }
        data.reset(userId);
        users.delete(u);
    }
}
