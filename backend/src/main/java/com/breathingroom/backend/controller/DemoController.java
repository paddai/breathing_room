package com.breathingroom.backend.controller;

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

    @PostMapping("/seed")
    public ResponseEntity<Void> seed() {
        demo.seed();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        demo.reset();
        return ResponseEntity.noContent().build();
    }
}
