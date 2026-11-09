package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.AdvisorService;
import com.breathingroom.backend.service.InsightsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InsightsController {

    private final InsightsService insights;
    private final AdvisorService advisor;

    public InsightsController(InsightsService insights, AdvisorService advisor) {
        this.insights = insights;
        this.advisor = advisor;
    }

    /** The health check: your numbers compared with published benchmarks. Pure calculation, no AI. */
    @GetMapping("/insights")
    public Dtos.InsightsDto insights() {
        return insights.view(CurrentUser.id());
    }

    /** Plain-language summary of the top priorities. Uses the AI if it is configured, otherwise built-in text. */
    @PostMapping("/insights/explain")
    public Dtos.InsightExplanation explain() {
        return advisor.explainInsights(CurrentUser.id());
    }

    /** Every outside number the app uses, with who published it, a link, and its date. */
    @GetMapping("/benchmarks")
    public List<Dtos.BenchmarkDto> benchmarks() {
        return insights.benchmarks();
    }
}
