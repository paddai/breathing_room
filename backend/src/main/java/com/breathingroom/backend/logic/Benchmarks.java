package com.breathingroom.backend.logic;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Published numbers from outside the app: limits, averages and guidelines, each with who published it, a link, and the date it is as of.
 * They are stored in code (not fetched live) so the app always works, every figure can be tested, and every figure can be traced.
 * To refresh them, edit this list and the "as of" text, then update docs/REFERENCE_DATA.md. Last reviewed: November 2, 2026.
 *
 * kind: OFFICIAL = published by a government agency, SURVEY = published survey or industry report,
 *       GUIDELINE = widely used rule of thumb, ASSUMPTION = a planning assumption chosen by this app (not a forecast).
 */
public final class Benchmarks {

    public record Benchmark(String id, String label, double value, String unit, String kind, String asOf, String source, String url, String note) {}

    private static final String BLS_CE = "https://www.bls.gov/news.release/archives/cesan_10292026.htm";
    private static final String BLS_CE_ASOF = "2025 data (BLS Consumer Expenditures, released Oct 29, 2026)";
    private static final String VANGUARD = "https://workplace.vanguard.com/content/dam/inst/iig-transformation/insights/pdf/has/how-america-saves-2026.pdf";
    private static final String VANGUARD_ASOF = "2025 plan data (Vanguard How America Saves 2026, published June 2026)";
    private static final String CFPB_DTI = "https://www.consumerfinance.gov/ask-cfpb/what-is-a-debt-to-income-ratio-en-1791/";
    private static final String HUD_CRS = "https://www.everycrsreport.com/reports/R48945.html";
    private static final String RULE_5030 = "https://americanexpress.com/en-us/credit-cards/credit-intel/how-much-to-save";

    private static final List<Benchmark> ALL = List.of(
            new Benchmark("irs_401k_limit", "IRS limit on employee 401(k) contributions", 24_500, "dollars per year", "OFFICIAL",
                    "2026 tax year (announced Nov 13, 2025)", "Internal Revenue Service, news release IR-2025-111",
                    "https://www.irs.gov/newsroom/401k-limit-increases-to-24500-for-2026-ira-limit-increases-to-7500",
                    "Your employer's match does not count toward this limit."),
            new Benchmark("irs_401k_catchup", "IRS extra catch-up for people 50 or older", 8_000, "dollars per year", "OFFICIAL",
                    "2026 tax year (announced Nov 13, 2025)", "Internal Revenue Service, news release IR-2025-111",
                    "https://www.irs.gov/newsroom/401k-limit-increases-to-24500-for-2026-ira-limit-increases-to-7500",
                    "A higher catch-up applies at ages 60 to 63."),
            new Benchmark("fed_card_apr", "Average credit card rate on accounts that carry a balance", 22.36, "percent APR", "OFFICIAL",
                    "August 2026 (Federal Reserve G.19, released Oct 7, 2026)", "Federal Reserve Board, G.19 Consumer Credit",
                    "https://www.federalreserve.gov/releases/g19/current/", "The average across all cards, including ones paid off monthly, is lower."),
            new Benchmark("cpi_12mo", "Inflation over the last 12 months (CPI-U, all items)", 3.7, "percent", "OFFICIAL",
                    "September 2026 (BLS, released Oct 14, 2026)", "U.S. Bureau of Labor Statistics, Consumer Price Index",
                    "https://www.bls.gov/news.release/cpi.nr0.htm", "A recent one-year figure, not a forecast. It has moved a lot during 2026."),
            new Benchmark("bls_housing_share", "Typical share of household spending on housing", 33.9, "percent of spending", "OFFICIAL",
                    BLS_CE_ASOF, "U.S. Bureau of Labor Statistics, Consumer Expenditures", BLS_CE, "Includes utilities. Shares are of all spending, including taxes and insurance."),
            new Benchmark("bls_transport_share", "Typical share of household spending on transportation", 16.5, "percent of spending", "OFFICIAL",
                    BLS_CE_ASOF, "U.S. Bureau of Labor Statistics, Consumer Expenditures", BLS_CE, "Cars, fuel, repairs and public transit."),
            new Benchmark("bls_food_share", "Typical share of household spending on food", 13.0, "percent of spending", "OFFICIAL",
                    BLS_CE_ASOF, "U.S. Bureau of Labor Statistics, Consumer Expenditures", BLS_CE, "Groceries and eating out together."),
            new Benchmark("hud_cost_burden", "Housing cost line: 'cost burdened' above this share of income", 30, "percent of income", "GUIDELINE",
                    "Long-standing federal standard (2024 data)", "HUD standard, as explained by the Congressional Research Service", HUD_CRS,
                    "Counts housing costs including utilities."),
            new Benchmark("hud_severe", "Housing cost line: 'severely cost burdened' above this share of income", 50, "percent of income", "GUIDELINE",
                    "Long-standing federal standard (2024 data)", "HUD standard, as explained by the Congressional Research Service", HUD_CRS,
                    "Counts housing costs including utilities."),
            new Benchmark("dti_lender", "Debt-to-income ratio lenders generally want to see", 36, "percent of gross income", "GUIDELINE",
                    "Current CFPB guidance", "Consumer Financial Protection Bureau", CFPB_DTI, "Total monthly debt payments divided by gross monthly income."),
            new Benchmark("dti_renter", "Debt payments the CFPB suggests for renters (not counting rent)", 20, "percent of gross income", "GUIDELINE",
                    "Current CFPB guidance", "Consumer Financial Protection Bureau", CFPB_DTI, "The CFPB suggests aiming for 15% to 20% or lower."),
            new Benchmark("emergency_months_low", "Emergency savings rule of thumb: low end", 3, "months of essential costs", "GUIDELINE",
                    "Widely used guidance", "Financial planners, as reported by CNBC", "https://www.cnbc.com/2024/03/12/cfp-how-much-money-to-start-with-in-your-emergency-fund.html",
                    "Starting with $500 to $1,000 is a good first step."),
            new Benchmark("emergency_months_high", "Emergency savings rule of thumb: high end", 6, "months of essential costs", "GUIDELINE",
                    "Widely used guidance", "Financial planners, as reported by CNBC", "https://www.cnbc.com/2024/03/12/cfp-how-much-money-to-start-with-in-your-emergency-fund.html",
                    "Single-income households and variable pay often aim higher."),
            new Benchmark("shed_cover_400", "Adults who could cover a $400 emergency with cash or its equivalent", 63, "percent of adults", "SURVEY",
                    "October 2025 survey (Federal Reserve, published May 13, 2026)", "Federal Reserve Board, Economic Well-Being of U.S. Households in 2025",
                    "https://www.federalreserve.gov/publications/2026-economic-well-being-of-us-households-in-2025-executive-summary.htm", "Unchanged from 2024."),
            new Benchmark("vanguard_total_rate", "Average total 401(k) saving rate, employee plus employer", 12.1, "percent of pay", "SURVEY",
                    VANGUARD_ASOF, "Vanguard, How America Saves 2026", VANGUARD, "Median was 11.6%."),
            new Benchmark("vanguard_avg_deferral", "Average employee 401(k) contribution", 7.6, "percent of pay", "SURVEY",
                    VANGUARD_ASOF, "Vanguard, How America Saves 2026", VANGUARD, "Median was 6.6%."),
            new Benchmark("vanguard_target_low", "Suggested total retirement saving rate: low end", 12, "percent of pay", "GUIDELINE",
                    VANGUARD_ASOF, "Vanguard, How America Saves 2026", VANGUARD, "Includes employer contributions."),
            new Benchmark("vanguard_target_high", "Suggested total retirement saving rate: high end", 15, "percent of pay", "GUIDELINE",
                    VANGUARD_ASOF, "Vanguard, How America Saves 2026", VANGUARD, "Includes employer contributions."),
            new Benchmark("assumed_return", "Long-term investing return assumed by this app", 7, "percent per year", "ASSUMPTION",
                    "Planning assumption set by this app", "Not a published figure and not a forecast", "",
                    "Used only to compare debt interest with possible investing growth. Real returns vary a lot."),
            new Benchmark("rule_needs", "50/30/20 guideline: needs", 50, "percent of take-home pay", "GUIDELINE",
                    "Popular rule of thumb", "Budgeting guideline, as explained by American Express", RULE_5030, "Housing, utilities, food at home, transportation, minimum debt payments."),
            new Benchmark("rule_wants", "50/30/20 guideline: wants", 30, "percent of take-home pay", "GUIDELINE",
                    "Popular rule of thumb", "Budgeting guideline, as explained by American Express", RULE_5030, "Dining out, shopping and everything not a need."),
            new Benchmark("rule_savings", "50/30/20 guideline: savings and extra debt payments", 20, "percent of take-home pay", "GUIDELINE",
                    "Popular rule of thumb", "Budgeting guideline, as explained by American Express", RULE_5030, "Whatever is left after needs and wants."));

    private Benchmarks() {}

    public static List<Benchmark> all() {
        return ALL;
    }

    public static Benchmark get(String id) {
        return ALL.stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow(() -> new NoSuchElementException("Unknown benchmark: " + id));
    }

    public static double value(String id) {
        return get(id).value();
    }
}
