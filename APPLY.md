# Apply v5 (testing-week fixes)

v5 is v4 plus the fixes for all 7 of your test findings (see `docs/TEST_FINDINGS.md`). You already have v4 running, so:

1. Copy everything inside the `breathing-room-update` folder over your repo root and choose **Replace**. Use File Explorer or `Copy-Item -Recurse -Force`.
2. **No new dependency, no secret, no data clearing.** Spring adds the new `display_name` column and the `user_avatar` table by itself when the backend starts.
3. Stop the backend and run `.\mvnw.cmd spring-boot:run` again, then restart `npm run dev` in the frontend folder.
4. In IntelliJ use **File > Reload All from Disk** if new files look unknown.

**Quick retest (about 5 minutes)**
- Paycheck: Save with nothing filled in, and each empty required input should show its own red message. Federal tax, State tax, Social Security + Medicare and Health insurance should already be on the page, optional.
- Budget & Expenses, Debts, Goals: press the add button with nothing filled in. Each input should show a plain message, with no "Is the backend running?" text.
- Advisor page: the new **Financial health check** card should list items with severity labels and source links. Click **Where do these numbers come from?** to see the full table, and **Explain my top priorities** (built-in text if no AI key). The Dashboard shows a short teaser when something needs a look.
- Profile (sidebar): upload a picture, change the display name, refresh the page (they should stay), then Sign out. Sign out is also the button at the top of the sidebar.

---

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
