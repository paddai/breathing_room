package com.breathingroom.backend.controller;

import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.DemoDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoDataService demo;

    public DemoController(DemoDataService demo) {
        this.demo = demo;
    }

    /** Replaces the signed-in user's data with sample data. Other users are not touched. */
    @PostMapping("/seed")
    public ResponseEntity<Void> seed() {
        demo.seed(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    /** Deletes the signed-in user's data (the account itself stays). */
    @DeleteMapping
    public ResponseEntity<Void> reset() {
        demo.reset(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
