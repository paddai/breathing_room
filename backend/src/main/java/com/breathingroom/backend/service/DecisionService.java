package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.DebtPayoff;
import com.breathingroom.backend.logic.GoalMath;
import com.breathingroom.backend.logic.ScenarioMath;
import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.model.PaycheckProfile;
import com.breathingroom.backend.repository.DebtRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Decision support (tasks 6.5-6.7 and 7.x): the payoff comparison and the 401(k)-vs-debt what-if. All numbers come from the logic package. */
@Service
public class DecisionService {

    private final FinanceService finance;
    private final DebtRepository debtRepo;

    public DecisionService(FinanceService finance, DebtRepository debtRepo) {
        this.finance = finance;
        this.debtRepo = debtRepo;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static Dtos.PayoffResult dto(DebtPayoff.Result r) {
        List<Dtos.PayoffStep> steps = new ArrayList<>();
        for (DebtPayoff.Step s : r.order()) steps.add(new Dtos.PayoffStep(s.name(), s.month()));
        return new Dtos.PayoffResult(r.strategy(), r.months(), r.paidOff(), GoalMath.round2(r.totalInterest()), steps);
    }

    private List<DebtPayoff.Debt> savedDebts(Long userId) {
        List<Debt> rows = new ArrayList<>(debtRepo.findByUserId(userId));
        rows.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<DebtPayoff.Debt> out = new ArrayList<>();
        for (Debt d : rows) out.add(new DebtPayoff.Debt(FinanceService.clip(d.getName(), 60), d.getBalance(), d.getApr(), d.getMinPayment()));
        return out;
    }

    /** Uses the debts in the request (sample mode) or, when none are sent, the signed-in user's saved debts. */
    public Dtos.PayoffComparison payoff(Long userId, Dtos.PayoffRequest req) {
        double extra = req == null || req.extra() == null ? 0 : req.extra();
        if (extra < 0 || extra > 1_000_000) throw bad("Extra payment must be between $0 and $1,000,000.");

        List<DebtPayoff.Debt> debts;
        if (req != null && req.debts() != null) {
            if (req.debts().size() > 20) throw bad("Compare up to 20 debts at a time.");
            debts = new ArrayList<>();
            int n = 1;
            for (Dtos.DebtInput d : req.debts()) {
                if (d.balance() < 0 || d.balance() > 100_000_000 || d.apr() < 0 || d.apr() > 100 || d.minPayment() < 0 || d.minPayment() > 10_000_000) {
                    throw bad("A debt has a balance, APR or minimum payment that is out of range.");
                }
                String name = d.name() == null || d.name().isBlank() ? "Debt " + n : FinanceService.clip(d.name(), 60);
                debts.add(new DebtPayoff.Debt(name, d.balance(), d.apr(), d.minPayment()));
                n++;
            }
        } else {
            debts = savedDebts(userId);
        }

        List<DebtPayoff.Debt> active = debts.stream().filter(d -> d.balance() > 0).toList();
        double total = active.stream().mapToDouble(DebtPayoff.Debt::balance).sum();
        if (active.isEmpty()) {
            return new Dtos.PayoffComparison(extra, 0, 0, null, null, null, "Add your debts to see a payoff plan.");
        }
        DebtPayoff.Result av = DebtPayoff.simulate(active, extra, DebtPayoff.Strategy.AVALANCHE);
        DebtPayoff.Result sb = DebtPayoff.simulate(active, extra, DebtPayoff.Strategy.SNOWBALL);
        DebtPayoff.Result mo = DebtPayoff.minimumsOnly(active);

        String message;
        if (!av.paidOff() || !sb.paidOff()) {
            message = "At this payment level your debts are not paid off within 50 years. A larger extra payment, or a call to your lender, may help.";
        } else if (sb.totalInterest() - av.totalInterest() > 0.5) {
            message = "Avalanche saves about " + GoalMath.money(sb.totalInterest() - av.totalInterest()) + " in interest compared with snowball.";
        } else if (av.totalInterest() - sb.totalInterest() > 0.5) {
            message = "Snowball costs about " + GoalMath.money(av.totalInterest() - sb.totalInterest()) + " less interest than avalanche here.";
        } else {
            message = "Both strategies cost about the same, so pick the one you will stick with.";
        }
        return new Dtos.PayoffComparison(extra, active.size(), GoalMath.round2(total), dto(av), dto(sb), dto(mo), message);
    }

    /** The what-if for the signed-in user: lower the 401(k) to the employer match and send the difference to debt. No AI is involved. */
    public Dtos.ScenarioDto scenario(Long userId, double afterTaxPercent) {
        if (afterTaxPercent < 0 || afterTaxPercent > 100) throw bad("After-tax percent must be between 0 and 100.");
        PaycheckProfile p = finance.profile(userId);
        ScenarioMath.Scenario s = ScenarioMath.compute(FinanceService.grossPerPeriod(p), FinanceService.periodsPerYear(p.getFrequency()),
                p.getContribution401kPercent(), p.getEmployerMatchLimit(), afterTaxPercent, savedDebts(userId));
        return new Dtos.ScenarioDto(s.applicable(), s.note(), GoalMath.round2(s.freedGrossPerMonth()), GoalMath.round2(s.extraToDebtPerMonth()),
                s.afterTaxPercent(), s.targetDebt(), s.targetApr(), s.current() == null ? null : dto(s.current()),
                s.alternative() == null ? null : dto(s.alternative()), s.monthsSaved(), GoalMath.round2(s.interestSaved()),
                GoalMath.round2(s.retirementGivenUp()), s.caution());
    }
}
