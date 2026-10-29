# Security notes for Breathing Room

This answers the reviewers' questions about how financial data is protected. It lists what is done, what is not, and why.
Breathing Room is a student capstone. It has not had an independent security review or penetration test.

## What data is stored

Your email, a password hash, and what you type into the forms: paycheck figures, deductions, expenses, debts, goals, and the
decisions you approved or declined. It does **not** store bank logins, card numbers, Social Security numbers, or account numbers,
and it never connects to a bank.

## What is protected, and how

| Area | What the app does |
|---|---|
| Passwords | Stored only as BCrypt hashes (slow, salted). At least 10 characters, at most 72 bytes, common passwords refused. Never logged. |
| Guessing attacks | After 5 failed sign-ins for one email the account pauses sign-in for 10 minutes. Errors are generic ("Invalid email or password") and an unknown email takes as long as a real one, so timing does not reveal who has an account. |
| Sessions | Signed HS256 tokens that expire after 8 hours. No cookies, so cross-site request forgery does not apply. The signing key comes from `JWT_SECRET` (32+ characters, kept out of Git). |
| Who can see what | Every row stores its owner. Every query filters by the signed-in user's id, and the browser can never choose an id or an owner. Another person's row is simply "not found". |
| Injection | The database layer uses parameterized queries (Spring Data JPA). Numbers, dates, text lengths, and percentages are checked on the server, not only in the browser. |
| Errors | Users see our own short messages. Stack traces are never sent to the browser. |
| Browser access | CORS allows only the origins listed in `CORS_ALLOWED_ORIGINS` (default: the local dev site). |
| Secrets | `.env` files are git-ignored. The OpenAI key lives only on the server and is never in the React code. |
| Your control | "Delete all my data" and "Delete my account" (asks for your password again) remove everything the account owns. |

## The AI part (OpenAI)

- Java calculates every number and builds every option. The AI only explains, or picks one option Java created.
- If an AI reply contains a dollar figure Java did not calculate, the reply is thrown away and built-in text is shown.
- The AI cannot change anything. Only the signed-in user's approval does, and the server re-checks the option first.
- What is sent to OpenAI: summary figures for the question being explained (for example a goal name, its target, and monthly amounts).
  Not the email, password, or name.
- Each account can trigger a limited number of AI replies per day (default 40) to protect the OpenAI bill.
- Prompts and keys are never logged.

## Known limits (be honest about these)

- **No email check and no password reset.** Anyone can sign up with any email, and a forgotten password cannot be recovered yet.
- **No two-step sign-in.**
- **The sign-in pause lives in memory and is per email.** It resets when the backend restarts, and there is no per-IP limit or limit on sign-ups.
- **Tokens cannot be revoked early.** Signing out clears the browser's copy, but a stolen token works until it expires.
- **The token is kept in the browser's session storage** (cleared when the tab closes). Script injected into the page could read it, so never add untrusted scripts.
- **Local use runs over plain HTTP.** Any real deployment must sit behind HTTPS.
- **Money values are stored as `double`.** Fine for a prototype; use exact decimals before production.
- **No automated test yet proves that user A cannot read user B's data.** It is on the to-do list (9.x).
- **The database itself is not encrypted by the app.** Disk encryption and backups are the host's job.
- **Not independently reviewed or penetration-tested.**

## Before any real deployment

HTTPS, a strong `JWT_SECRET` from a secret manager, `CORS_ALLOWED_ORIGINS` set to the real site, a database user with limited rights,
backups, a privacy policy, email verification and password reset, per-IP rate limiting, and a security review.

## Self-review checklist (task 14.3)

| Item | Status |
|---|---|
| Authentication | Built. Needs your testing. |
| Per-user data | Built. Needs a two-account test (see APPLY.md). |
| Input validation | Done for all forms. |
| Query safety | Parameterized by the framework. |
| CORS restricted | Built (configurable). |
| HTTPS | Documented, not done (local only). |
| Secrets out of Git | Done (`.env` ignored, `.env.example` committed). |
| AI data minimized, approval required | Done. |
| Cross-user access tests | Not done. |
| Password reset, email verification, MFA | Not done. |
