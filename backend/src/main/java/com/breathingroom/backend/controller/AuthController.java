package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public Dtos.AuthResponse register(@RequestBody Dtos.AuthRequest body) {
        return auth.register(body);
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@RequestBody Dtos.AuthRequest body) {
        return auth.login(body);
    }

    @GetMapping("/me")
    public Dtos.UserDto me() {
        return auth.me(CurrentUser.id());
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(@RequestBody Dtos.PasswordRequest body) {
        auth.deleteAccount(CurrentUser.id(), body.password());
        return ResponseEntity.noContent().build();
    }
}
