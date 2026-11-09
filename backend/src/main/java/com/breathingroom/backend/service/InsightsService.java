package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.Benchmarks;
import com.breathingroom.backend.logic.DebtPayoff;
import com.breathingroom.backend.logic.InsightsEngine;
import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.model.PaycheckProfile;
import com.breathingroom.backend.repository.DebtRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Gathers the signed-in user's numbers and compares them with the published benchmarks in Benchmarks. No AI here. */
@Service
public class InsightsService {

    private final FinanceService finance;
    private final DebtRepository debtRepo;

    public InsightsService(FinanceService finance, DebtRepository debtRepo) {
        this.finance = finance;
        this.debtRepo = debtRepo;
    }

    public List<InsightsEngine.Insight> analyze(Long userId) {
        PaycheckProfile p = finance.profile(userId);
        Dtos.SummaryDto s = finance.summary(userId);
        List<DebtPayoff.Debt> debts = new ArrayList<>();
        for (Debt d : debtRepo.findByUserId(userId)) debts.add(new DebtPayoff.Debt(FinanceService.clip(d.getName(), 60), d.getBalance(), d.getApr(), d.getMinPayment()));
        List<InsightsEngine.GoalFact> goals = new ArrayList<>();
        for (Dtos.GoalView g : finance.goalViews(userId)) goals.add(new InsightsEngine.GoalFact(g.name(), g.targetAmount(), g.savedAmount(), g.monthsLeft(), g.monthlyContribution()));
        InsightsEngine.Facts facts = new InsightsEngine.Facts(FinanceService.grossPerPeriod(p), FinanceService.periodsPerYear(p.getFrequency()), s.monthlyTakeHome(),
                p.getContribution401kPercent(), p.getEmployerMatchLimit(), p.getEmployerMatchRate(), s.spendingByCategory(), s.monthlyExpenses(), s.monthlyDebtMinimums(), debts, goals);
        return InsightsEngine.analyze(facts);
    }

    public Dtos.InsightsDto view(Long userId) {
        List<Dtos.InsightDto> out = new ArrayList<>();
        int attention = 0;
        for (InsightsEngine.Insight i : analyze(userId)) {
            List<Dtos.SourceRef> sources = new ArrayList<>();
            for (String id : i.sources()) {
                Benchmarks.Benchmark b = Benchmarks.get(id);
                sources.add(new Dtos.SourceRef(b.id(), b.source(), b.url(), b.asOf()));
            }
            if (i.severity().equals("ALERT") || i.severity().equals("WATCH")) attention++;
            out.add(new Dtos.InsightDto(i.id(), i.severity(), i.title(), i.detail(), sources));
        }
        return new Dtos.InsightsDto("Compared with published benchmarks reviewed on November 2, 2026. Educational only, not financial advice.", attention, out);
    }

    public List<Dtos.BenchmarkDto> benchmarks() {
        List<Dtos.BenchmarkDto> out = new ArrayList<>();
        for (Benchmarks.Benchmark b : Benchmarks.all()) {
            out.add(new Dtos.BenchmarkDto(b.id(), b.label(), b.value(), b.unit(), b.kind(), b.asOf(), b.source(), b.url(), b.note()));
        }
        return out;
    }
}
