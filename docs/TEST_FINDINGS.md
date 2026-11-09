# Test findings and fixes (testing week, Oct 26 to Nov 1, 2026)

Findings from hands-on testing of the running app, and what was changed. "Retest" means it still needs a quick pass on your computer.

| # | Finding | Status | What changed | Where |
|---|---|---|---|---|
| 1 | When Save cannot be applied, show the error on the input with the reason | Fixed, retest | Every form checks in the browser first, then the server checks again. Each problem input turns red and shows its own message (for example "Enter your hourly rate."). The server returns a `fieldErrors` answer naming the inputs. | `validate.js`, `ui.jsx` (Field), `ApiExceptionHandler`, `FinanceService` |
| 2 | No way to sign out; add a profile page with a profile picture | Fixed, retest | New Profile page (name, picture, Sign out), a user badge and Sign out button at the top of the sidebar, picture upload with file-signature check and size limit. | `ProfilePage.jsx`, `App.jsx`, `ProfileController`, `ProfileService`, `ImageCheck` |
| 3 | Make the logic and AI advice more complex using financial data from outside, not only what the user types | Fixed, retest | New Financial health check on the Advisor page (and a teaser on the Dashboard): 11 rules compare the user's numbers with published figures (IRS 401(k) limit, Federal Reserve card rate, BLS inflation and spending shares, HUD housing line, CFPB debt-to-income, Fed emergency-savings survey, Vanguard saving rates). Goal advice now includes an inflation-adjusted target. The AI sees these figures and must name the publisher; wording with an unknown dollar or percent figure is rejected. Every figure is listed with its source and date in `docs/REFERENCE_DATA.md` and in the app. | `Benchmarks`, `InsightsEngine`, `NumberGuard`, `InsightsService`, `HealthCheck.jsx` |
| 4 | Add expense with nothing filled in shows an unfriendly error | Fixed, retest | Description and category must be chosen (the category now starts on "Choose a category..."). Messages appear under each input. | `ExpenseForm.jsx`, `ExpenseController` |
| 5 | Same unfriendly error on the Debts tab | Fixed, retest | Same approach: name, balance, APR and minimum payment each get their own message. | `DebtForm.jsx`, `DebtController` |
| 6 | Same for adding Goals without data | Fixed, retest | Name, target, date (must be in the future) and amounts each get their own message. "Add savings" shows its message under the goal. | `GoalsPage.jsx`, `GoalService` |
| 7 | Payroll deductions should be added automatically and accept blank | Fixed, retest | Federal tax, State tax, Social Security + Medicare and Health insurance are always on the page as optional amounts. Blank rows are ignored. "Other deductions" lets you add extras. | `PaycheckForm.jsx` |

Also fixed along the way: the page-load error box no longer says "Is the backend running?" for every error. It says so only when the server cannot be reached.
