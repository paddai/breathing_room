# Test findings and fixes (testing weeks, Oct 26 to Nov 8, 2026)

Findings from hands-on testing of the running app, and what was changed. "Retest" means it still needs a quick pass on your computer.

## Version history of these findings
- **v4 initial run-through (Oct 28 to Nov 1, 2026):** reported the seven findings below.
- **v5 (built Nov 1, 2026):** fixes for all seven. The Maven build succeeded and the 40 automated tests passed on the developer's computer (Nov 8).
- **Browser retest of the seven findings in v5 (week of Nov 2 to 8, 2026):** passed. All seven findings work as intended.
- **New finding 8 from the same retest:** creating an account with an email that already exists showed a bare "409". Fixed in v5.1 (Nov 14, 2026). Retest pending.

| # | Finding | Status | What changed | Where |
|---|---|---|---|---|
| 1 | When Save cannot be applied, show the error on the input with the reason | Fixed, retested OK | Every form checks in the browser first, then the server checks again. Each problem input turns red and shows its own message (for example "Enter your hourly rate."). The server returns a `fieldErrors` answer naming the inputs. | `validate.js`, `ui.jsx` (Field), `ApiExceptionHandler`, `FinanceService` |
| 2 | No way to sign out; add a profile page with a profile picture | Fixed, retested OK | New Profile page (name, picture, Sign out), a user badge and Sign out button at the top of the sidebar, picture upload with file-signature check and size limit. | `ProfilePage.jsx`, `App.jsx`, `ProfileController`, `ProfileService`, `ImageCheck` |
| 3 | Make the logic and AI advice more complex using financial data from outside, not only what the user types | Fixed, retested OK | New Financial health check on the Advisor page (and a teaser on the Dashboard): 11 rules compare the user's numbers with published figures (IRS 401(k) limit, Federal Reserve card rate, BLS inflation and spending shares, HUD housing line, CFPB debt-to-income, Fed emergency-savings survey, Vanguard saving rates). Goal advice now includes an inflation-adjusted target. The AI sees these figures and must name the publisher; wording with an unknown dollar or percent figure is rejected. Every figure is listed with its source and date in `docs/REFERENCE_DATA.md` and in the app. | `Benchmarks`, `InsightsEngine`, `NumberGuard`, `InsightsService`, `HealthCheck.jsx` |
| 4 | Add expense with nothing filled in shows an unfriendly error | Fixed, retested OK | Description and category must be chosen (the category now starts on "Choose a category..."). Messages appear under each input. | `ExpenseForm.jsx`, `ExpenseController` |
| 5 | Same unfriendly error on the Debts tab | Fixed, retested OK | Same approach: name, balance, APR and minimum payment each get their own message. | `DebtForm.jsx`, `DebtController` |
| 6 | Same for adding Goals without data | Fixed, retested OK | Name, target, date (must be in the future) and amounts each get their own message. "Add savings" shows its message under the goal. | `GoalsPage.jsx`, `GoalService` |
| 7 | Payroll deductions should be added automatically and accept blank | Fixed, retested OK | Federal tax, State tax, Social Security + Medicare and Health insurance are always on the page as optional amounts. Blank rows are ignored. "Other deductions" lets you add extras. | `PaycheckForm.jsx` |
| 8 | Creating an account with an email that already exists shows only "409" | Fixed in v5.1, retest | Cause: Spring leaves the reason out of the JSON body of a `ResponseStatusException`, so the browser only had the number. Now the server answers 409 with `fieldErrors.email`, the sign-up screen shows "An account with this email already exists. Sign in instead." under the email box, and a small button switches to Sign in. The same handler now passes the reason of every other sign-in and sign-up error through to the screen. Trade-off noted in `docs/SECURITY.md` (user enumeration). | `FieldValidationException`, `ApiExceptionHandler`, `AuthService`, `LoginPage.jsx`, `ApiExceptionHandlerTest` |

Also fixed along the way: the page-load error box no longer says "Is the backend running?" for every error. It says so only when the server cannot be reached.

## Round 3 (Nov 15) - findings 9 to 12 (tracker tasks 15.14 to 15.19)

| # | Finding | Fix | Status |
|---|---------|-----|--------|
| 9 | Add Annually as a salary pay frequency | `PaycheckMath` 1 period per year; Annually is salary-only (server and form check); form labels switch to "per year" | Fixed, retest pending |
| 10 | Expense categories were too few | 15 categories in the form and a server-side whitelist (`ExpenseCategories`); Savings no longer counted as a "want" in 50/30/20 | Fixed, retest pending |
| 11 | Existing email showed a raw 409 | Exact message: "An account with this email already exists. Please sign in or use a different email." shown under the email field | Fixed, retest pending |
| 12 | Advisor answers repeated entered data | `AdvisorPlanner` works out 2-3 options with trade-offs in Java (debt, 401(k) and match, savings, spending); AI explains and adds context; Anthropic provider added (OpenAI kept); dollar and percent guards kept | Fixed, retest pending |

Retest checklist: see the reply that came with this patch (manual testing steps).

| 13 | Editing the retirement (401(k)) boxes did not change the Advisor or other pages | The values are saved and read fresh by every page, but only when Save paycheck is pressed. Added an "unsaved changes" warning and a tab-close warning; the Advisor 401(k) card now shows your current contribution and match | Fixed, retest pending |
| 14 | Hourly could not be chosen after Annually | The Hourly button was locked while Annually was selected (my change). Now choosing Hourly switches the frequency to Biweekly with a note | Fixed, retest pending |
