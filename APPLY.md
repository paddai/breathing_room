# Apply this update (v4: complete implementation)

v4 contains everything from v2 and v3, so you only need this one bundle. If you already applied v3, skip steps 1, 3 and 4 below (no new dependency,
no database reset, no new secret): just copy the files (step 2) and restart the backend and frontend.

**New in v4**
- The payoff comparison (snowball, avalanche, minimums only) and the live take-home preview are now calculated by the Java server, not the browser.
- 401(k) vs Debt has a real what-if scenario: keep your 401(k) vs lower it to the match and send the difference to debt (months, interest, 401(k) given up).
- `docs/user-flow.png` (task 2.3) and three new endpoints in `docs/API.md`.
- More tests in `EnginesTest.java` for next week's testing phase.

## 1. Add one dependency to `backend/pom.xml`
Inside `<dependencies>`, add:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
Then reload Maven (IntelliJ: the small Maven "reload" button, or `mvnw.cmd clean compile`).

## 2. Copy the files
Copy everything INSIDE the `breathing-room-update` folder into your repo root and choose **Replace**. It adds/replaces files under
`backend\`, `frontend\`, `docs\`, plus `README.md` and `.gitignore`. It does not touch your `HealthController.java` or `BackendApplication.java`.

## 3. Clear the old data (once)
Existing rows have no owner, so they would be invisible (and the old paycheck row would attach to the first account). In DataGrip, open a
console on the `breathingroom` database and run:
```sql
TRUNCATE TABLE goal_decision, goal, debt, expense, deduction, paycheck_profile RESTART IDENTITY;
```
If a table does not exist yet, remove it from the list. The new `app_user` table and the `user_id` columns are created automatically when the backend starts.

## 4. Make a signing secret
In `backend\.env` add a line `JWT_SECRET=` followed by a long random value. In PowerShell:
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```
Paste the output after `JWT_SECRET=`. Never commit this file (it is git-ignored). Without a secret the app still runs but signs everyone out on each restart.

## 5. Run it
Start the backend, then restart the frontend (`npm run dev`). Open http://localhost:5173, choose **Create account**, then **Settings > Load sample data**.

## 6. Test it (this is what turns the to-do items green)
1. Wrong password: you see "Invalid email or password", and after 5 misses a pause message.
2. Two accounts: sign up in a normal window, load sample data. In a private window sign up with another email. The second account must be **empty**.
3. Sign out, then try to open http://localhost:8080/api/summary in the browser. It must say 401, not show data.
4. Settings > Delete my account removes the account, and signing in again fails.

## If something goes wrong
- Backend will not start, "JWT_SECRET must be at least 32 characters": make the secret longer.
- Compile error: paste it to me with the file name and I will fix it. I could not run a real Maven build.
- Every page says "Please sign in": your session expired or the backend restarted without `JWT_SECRET`. Sign in again.
