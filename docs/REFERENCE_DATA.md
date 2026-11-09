# Outside financial data (reference data)

The Financial health check compares what you entered with published figures from outside the app. This page lists every one of them, who published it, and its date.
The table below is generated from `Benchmarks.java`, so it matches the app exactly.

## Design decisions
- **Stored in the app, not fetched live.** The app always works offline, every figure can be tested, and every figure can be traced to a source and date. A live feed (for example the FRED service) could replace this later; that would add an API key and failure cases.
- **Java compares, the AI explains.** `InsightsEngine` makes every comparison. The AI (if a key is set) may only explain the results. Its wording is rejected if it contains a dollar or percent figure that Java did not supply (`NumberGuard`).
- **Kinds of figures.** OFFICIAL = government agency; SURVEY = published survey or industry report; GUIDELINE = widely used rule of thumb; ASSUMPTION = a planning assumption chosen by this app (the 7% investing return). Assumptions are labeled everywhere they appear.
- **Nothing about the user is sent anywhere to get these figures.** They are constants in the program.

## The figures (reviewed November 2, 2026)

| Id | Figure | Value | Kind | Published by / as of | Link |
|---|---|---|---|---|---|
| `irs_401k_limit` | IRS limit on employee 401(k) contributions | 24500.0 (dollars per year) | OFFICIAL | Internal Revenue Service, news release IR-2025-111; 2026 tax year (announced Nov 13, 2025) | https://www.irs.gov/newsroom/401k-limit-increases-to-24500-for-2026-ira-limit-increases-to-7500 |
| `irs_401k_catchup` | IRS extra catch-up for people 50 or older | 8000.0 (dollars per year) | OFFICIAL | Internal Revenue Service, news release IR-2025-111; 2026 tax year (announced Nov 13, 2025) | https://www.irs.gov/newsroom/401k-limit-increases-to-24500-for-2026-ira-limit-increases-to-7500 |
| `fed_card_apr` | Average credit card rate on accounts that carry a balance | 22.36 (percent APR) | OFFICIAL | Federal Reserve Board, G.19 Consumer Credit; August 2026 (Federal Reserve G.19, released Oct 7, 2026) | https://www.federalreserve.gov/releases/g19/current/ |
| `cpi_12mo` | Inflation over the last 12 months (CPI-U, all items) | 3.7 (percent) | OFFICIAL | U.S. Bureau of Labor Statistics, Consumer Price Index; September 2026 (BLS, released Oct 14, 2026) | https://www.bls.gov/news.release/cpi.nr0.htm |
| `bls_housing_share` | Typical share of household spending on housing | 33.9 (percent of spending) | OFFICIAL | U.S. Bureau of Labor Statistics, Consumer Expenditures; 2025 data (BLS Consumer Expenditures, released Oct 29, 2026) | https://www.bls.gov/news.release/archives/cesan_10292026.htm |
| `bls_transport_share` | Typical share of household spending on transportation | 16.5 (percent of spending) | OFFICIAL | U.S. Bureau of Labor Statistics, Consumer Expenditures; 2025 data (BLS Consumer Expenditures, released Oct 29, 2026) | https://www.bls.gov/news.release/archives/cesan_10292026.htm |
| `bls_food_share` | Typical share of household spending on food | 13.0 (percent of spending) | OFFICIAL | U.S. Bureau of Labor Statistics, Consumer Expenditures; 2025 data (BLS Consumer Expenditures, released Oct 29, 2026) | https://www.bls.gov/news.release/archives/cesan_10292026.htm |
| `hud_cost_burden` | Housing cost line: 'cost burdened' above this share of income | 30.0 (percent of income) | GUIDELINE | HUD standard, as explained by the Congressional Research Service; Long-standing federal standard (2024 data) | https://www.everycrsreport.com/reports/R48945.html |
| `hud_severe` | Housing cost line: 'severely cost burdened' above this share of income | 50.0 (percent of income) | GUIDELINE | HUD standard, as explained by the Congressional Research Service; Long-standing federal standard (2024 data) | https://www.everycrsreport.com/reports/R48945.html |
| `dti_lender` | Debt-to-income ratio lenders generally want to see | 36.0 (percent of gross income) | GUIDELINE | Consumer Financial Protection Bureau; Current CFPB guidance | https://www.consumerfinance.gov/ask-cfpb/what-is-a-debt-to-income-ratio-en-1791/ |
| `dti_renter` | Debt payments the CFPB suggests for renters (not counting rent) | 20.0 (percent of gross income) | GUIDELINE | Consumer Financial Protection Bureau; Current CFPB guidance | https://www.consumerfinance.gov/ask-cfpb/what-is-a-debt-to-income-ratio-en-1791/ |
| `emergency_months_low` | Emergency savings rule of thumb: low end | 3.0 (months of essential costs) | GUIDELINE | Financial planners, as reported by CNBC; Widely used guidance | https://www.cnbc.com/2024/03/12/cfp-how-much-money-to-start-with-in-your-emergency-fund.html |
| `emergency_months_high` | Emergency savings rule of thumb: high end | 6.0 (months of essential costs) | GUIDELINE | Financial planners, as reported by CNBC; Widely used guidance | https://www.cnbc.com/2024/03/12/cfp-how-much-money-to-start-with-in-your-emergency-fund.html |
| `shed_cover_400` | Adults who could cover a $400 emergency with cash or its equivalent | 63.0 (percent of adults) | SURVEY | Federal Reserve Board, Economic Well-Being of U.S. Households in 2025; October 2025 survey (Federal Reserve, published May 13, 2026) | https://www.federalreserve.gov/publications/2026-economic-well-being-of-us-households-in-2025-executive-summary.htm |
| `vanguard_total_rate` | Average total 401(k) saving rate, employee plus employer | 12.1 (percent of pay) | SURVEY | Vanguard, How America Saves 2026; 2025 plan data (Vanguard How America Saves 2026, published June 2026) | https://workplace.vanguard.com/content/dam/inst/iig-transformation/insights/pdf/has/how-america-saves-2026.pdf |
| `vanguard_avg_deferral` | Average employee 401(k) contribution | 7.6 (percent of pay) | SURVEY | Vanguard, How America Saves 2026; 2025 plan data (Vanguard How America Saves 2026, published June 2026) | https://workplace.vanguard.com/content/dam/inst/iig-transformation/insights/pdf/has/how-america-saves-2026.pdf |
| `vanguard_target_low` | Suggested total retirement saving rate: low end | 12.0 (percent of pay) | GUIDELINE | Vanguard, How America Saves 2026; 2025 plan data (Vanguard How America Saves 2026, published June 2026) | https://workplace.vanguard.com/content/dam/inst/iig-transformation/insights/pdf/has/how-america-saves-2026.pdf |
| `vanguard_target_high` | Suggested total retirement saving rate: high end | 15.0 (percent of pay) | GUIDELINE | Vanguard, How America Saves 2026; 2025 plan data (Vanguard How America Saves 2026, published June 2026) | https://workplace.vanguard.com/content/dam/inst/iig-transformation/insights/pdf/has/how-america-saves-2026.pdf |
| `assumed_return` | Long-term investing return assumed by this app | 7.0 (percent per year) | ASSUMPTION | Not a published figure and not a forecast; Planning assumption set by this app | none (an assumption) |
| `rule_needs` | 50/30/20 guideline: needs | 50.0 (percent of take-home pay) | GUIDELINE | Budgeting guideline, as explained by American Express; Popular rule of thumb | https://americanexpress.com/en-us/credit-cards/credit-intel/how-much-to-save |
| `rule_wants` | 50/30/20 guideline: wants | 30.0 (percent of take-home pay) | GUIDELINE | Budgeting guideline, as explained by American Express; Popular rule of thumb | https://americanexpress.com/en-us/credit-cards/credit-intel/how-much-to-save |
| `rule_savings` | 50/30/20 guideline: savings and extra debt payments | 20.0 (percent of take-home pay) | GUIDELINE | Budgeting guideline, as explained by American Express; Popular rule of thumb | https://americanexpress.com/en-us/credit-cards/credit-intel/how-much-to-save |


## What uses them (the rules in `InsightsEngine`)
| Rule | Compares your numbers with |
|---|---|
| 401(k) yearly pace | IRS contribution limit and catch-up |
| Employer match left unclaimed | your match settings only (no outside figure) |
| Total retirement saving rate | Vanguard average and suggested 12% to 15% |
| Debts above the average card rate | Federal Reserve G.19 average rate on balances |
| Debt versus investing | the 7% investing assumption |
| Monthly interest cost | your debts only |
| Debt-to-income ratio | CFPB guidance (36%; 15% to 20% for renters) |
| Housing share of gross income | HUD 30% and 50% lines |
| Spending mix | BLS Consumer Expenditures shares |
| 50/30/20 check | the 50/30/20 rule of thumb |
| Emergency fund | 3 to 6 months guideline and the Fed's $400 survey figure |
| Goals and inflation | BLS 12-month CPI (goals within 36 months only) |

## How to refresh the figures
1. Open `backend/src/main/java/com/breathingroom/backend/logic/Benchmarks.java` and edit the value, the "as of" text, and the link for the figure.
2. Update the "Last reviewed" date in that file and the note in `InsightsService.view`.
3. Run the tests. `BenchmarksAndGuardTest` checks that every figure has a source, a date and an https link, and `InsightsEngineTest` shows which rules use the numbers.
4. Regenerate this table.

Dates worth watching: the IRS announces next year's limits in the fall (2027 limits are due soon); the Federal Reserve updates credit card rates quarterly; BLS publishes CPI monthly (October 2026 data is scheduled for November 10, 2026); BLS Consumer Expenditures are annual.

## Limits to be honest about
- The BLS spending shares include taxes, insurance and pensions, while the app only sees what the user logs, so the comparison is rough.
- Housing and spending checks use entries logged **this month so far**, so early in the month they can look off.
- The inflation figure is one recent 12-month reading, not a forecast, and it has moved a lot during 2026. It is only applied to goals within 36 months.
- The emergency-fund months and the 50/30/20 split are rules of thumb, not official requirements.
