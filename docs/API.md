# Breathing Room REST API (task 2.5)

Base URL (local): `http://localhost:8080/api`. In the browser the React app calls `/api/...` and Vite forwards it to the backend.
All bodies are JSON. **Everything except the three public endpoints needs `Authorization: Bearer <token>`** (the token comes from register or login).

## Public

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/auth/register` | `{ "email", "password" }` | `{ token, expiresInSeconds, user: { id, email } }` |
| POST | `/auth/login` | `{ "email", "password" }` | same as register |
| GET | `/health` | none | plain text "Breathing Room API is running" |

## Account

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/auth/me` | none | `{ id, email }` |
| POST | `/auth/delete-account` | `{ "password" }` | 204. Deletes the account and all its data. |

## Money data (each user only ever sees their own rows)

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/paycheck` | none | paycheck settings and deductions |
| PUT | `/paycheck` | `{ payType, hourlyRate, regularHours, overtimeHours, overtimeMultiplier, salaryAmount, frequency, contribution401kPercent, employerMatchRate, employerMatchLimit, deductions: [{ name, amount }] }` | saved paycheck |
| GET | `/expenses` | none | list, newest first |
| POST | `/expenses` | `{ description, category, amount, expenseDate }` | created expense |
| DELETE | `/expenses/{id}` | none | 204 |
| GET | `/debts` | none | list |
| POST | `/debts` | `{ name, balance, apr, minPayment }` | created debt |
| DELETE | `/debts/{id}` | none | 204 |
| GET | `/summary` | none | take-home, spending this month, debt, free money, goals totals |
| GET | `/recommendation` | none | 401(k)-vs-debt facts and an explanation |

## Calculations (all done in Java; nothing here uses AI)

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/paycheck/preview` | the same body as `PUT /paycheck` | `{ grossPerPeriod, periodsPerYear, grossPerYear, totalDeductions, k401PerPeriod, employerMatchPerPeriod, takeHomePerPeriod, takeHomePerMonth }`. Saves nothing. |
| POST | `/debts/payoff` | `{ extra, debts? }` (leave out `debts` to use your saved debts) | `{ avalanche, snowball, minimumsOnly, message }`, each with months, paidOff, totalInterest and the payoff order |
| GET | `/recommendation/scenario?afterTax=100` | none | what-if: keep your 401(k) vs lower it to the employer match and send the difference to debt (months, interest, 401(k) given up) |

## Goals and advisor

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/goals` | none | goals with progress, months left, needed per month, status |
| POST | `/goals` | `{ name, targetAmount, savedAmount, targetDate, monthlyContribution }` | created goal |
| DELETE | `/goals/{id}` | none | 204 |
| POST | `/goals/{id}/savings` | `{ amount }` | updated goal |
| GET | `/goals/{id}/advice` | none | options built by Java, a suggested one, and an explanation. **Changes nothing.** |
| POST | `/goals/{id}/decision` | `{ optionId, status: "APPROVED" or "DECLINED" }` | updated goal. The option is re-checked on the server. |
| GET | `/goal-decisions` | none | the 15 most recent approvals and declines |
| GET | `/advisor/status` | none | `{ configured, model }` |
| POST | `/advisor/ask` | `{ question }` (300 characters max) | `{ answer, aiUsed, note }` |

## Sample data

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/demo/seed` | none | 204. Replaces the signed-in user's data with sample data. |
| DELETE | `/demo` | none | 204. Deletes the signed-in user's data (account stays). |

## Status codes

| Code | Meaning |
|---|---|
| 400 | A value failed validation. The `message` says what to fix. |
| 401 | Not signed in, token expired, or wrong email or password. |
| 404 | The goal does not exist (or belongs to someone else). |
| 409 | An account with that email already exists. |
| 429 | Too many failed sign-ins. Wait about 10 minutes. |
