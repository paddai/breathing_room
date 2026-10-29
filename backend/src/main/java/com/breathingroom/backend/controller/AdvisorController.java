package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.AdvisorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdvisorController {

    private final AdvisorService advisor;

    public AdvisorController(AdvisorService advisor) {
        this.advisor = advisor;
    }

    @GetMapping("/advisor/status")
    public Dtos.AiStatus status() {
        return advisor.status();
    }

    @GetMapping("/recommendation")
    public Dtos.RecommendationDto recommendation() {
        return advisor.recommendation(CurrentUser.id());
    }

    @PostMapping("/advisor/ask")
    public Dtos.AskResponse ask(@RequestBody Dtos.AskRequest body) {
        return advisor.ask(CurrentUser.id(), body.question());
    }
}
