# Breathing Room

*From paycheck pressure to financial clarity.* A budgeting and debt-payoff app (IST 440W capstone):
React + Spring Boot + PostgreSQL, with an optional OpenAI "explainer".

> **Design rule: Java calculates; OpenAI explains. You stay in control.**
> Every number comes from Java code. The AI only puts those numbers into plain language, or picks one of the
> options Java created. It can never change your data - only you can, by approving a suggestion.

## Run it on a new computer (about 10 minutes)

**You need:** Git, Java 17+, Node.js 20+, and a PostgreSQL database (Docker *or* a local install).

### 1. Get the code
```
git clone <repo-url>
cd <repo-folder>
```

### 2. Start the database (pick ONE)
- **Docker:** `docker compose up -d db`
- **Local PostgreSQL:** create an empty database named `breathingroom` (DataGrip or `createdb breathingroom`).

### 3. Start the backend (new terminal)
```
cd backend
copy .env.example .env          (Mac/Linux: cp .env.example .env)
```
Open `backend/.env` and set `DB_PASSWORD` if you use your own PostgreSQL (Docker users: leave the defaults).
Then run:
```
mvnw.cmd spring-boot:run        (Mac/Linux: ./mvnw spring-boot:run)
```
Check it: <http://localhost:8080/api/health> should say "Breathing Room API is running".

### 4. Start the frontend (another terminal)
```
cd frontend
npm install
npm run dev
```
Open <http://localhost:5173>, choose **Create account**, then go to **Settings -> Load sample data** and explore every page.
Each account only ever sees its own data.

## Turn on the AI explanations (optional)
Put your key in `backend/.env` as `OPENAI_API_KEY=...` and restart the backend. Without a key the app works the
same, using built-in explanations. The key stays on the server and is git-ignored - never paste it into React code.

## What is in the app
| Page | What it does |
|---|---|
| Dashboard | Free money each month, spending by category, goals, debt, 401(k) alert |
| Paycheck | Gross -> take-home pay, deductions, 401(k) and employer match (calculated by the server, saved in the database) |
| Budget & Expenses | Log expenses, see income vs. spending vs. debt minimums |
| Debts / Debt Payoff | Track debts; the server compares avalanche, snowball and minimums-only, month by month |
| 401(k) vs Debt | Flags contributions above the employer match, then shows a what-if: keep your 401(k) vs lower it to the match and pay debt faster |
| Goals | Savings goals with required monthly amount and status |
| Advisor | Goal suggestions you approve or decline (with an audit log), plus "ask about my numbers" |
| Settings | Sample data, AI status, privacy notes |

## How the AI is kept safe
1. Java computes all numbers and the list of options. The AI cannot invent an option.
2. The AI reply is checked: any `$` figure Java did not calculate -> the reply is thrown away and built-in text is shown.
3. Approving a suggestion re-validates the option on the server. The browser's numbers are never trusted.
4. No key / no internet / API error -> the app keeps working with built-in text.
5. The key never leaves the backend; prompts and keys are never logged.
6. Every approve/decline is recorded and shown on the Advisor page.

## Tests
```
cd backend
mvnw.cmd test                   (Mac/Linux: ./mvnw test)
```
Covers the goal math and the AI "dollar guard".

## Known limitations (tracked in the to-do list)
- Accounts have no email check, password reset, or two-step sign-in yet. See `docs/SECURITY.md` for what is and is not protected.
- Money is stored as `double` for the prototype; a production app should use `BigDecimal`.
- HTTP only (local use). HTTPS and tighter CORS are needed before any real deployment.
- Educational estimates only - not financial, tax, or investment advice.

## Where the math lives
Every money calculation is Java code in `backend/src/main/java/com/breathingroom/backend/logic/`: `PaycheckMath` (take-home pay), `DebtPayoff` (snowball and
avalanche), `ScenarioMath` (the 401(k) what-if), `GoalMath` (goals) and `DollarGuard` (checks AI wording). The React pages only show what the server returns.
These classes have no Spring or database code, so they are easy to unit test.

## Documents
`docs/API.md` (every REST endpoint), `docs/SECURITY.md` (how data is protected and what is not), `docs/user-flow.png` (user flow diagram).

## Accounts and security
Sign-up and sign-in use BCrypt-hashed passwords and signed tokens, and every row belongs to a user. Details, limits, and a deployment checklist are in
`docs/SECURITY.md`. The REST endpoints are listed in `docs/API.md`. Set `JWT_SECRET` in `backend/.env` (see `.env.example`).

## Project layout
```
backend/   Spring Boot (controller -> service -> repository -> PostgreSQL), logic/ = pure, tested math
frontend/  React + Vite; /api requests are proxied to the backend (vite.config.js)
docker-compose.yml   optional PostgreSQL
```
