# Apply this update to your repo (Windows)

1. In IntelliJ/terminal make sure your current work is committed first (see step 0 in the chat instructions).
2. Unzip this folder. Copy everything INSIDE `breathing-room-update` into your repo root
   (`C:\Users\padda\breakthrough_app`), choosing **Replace** when asked. Do NOT copy the folder itself.
   - It adds/replaces: `.gitignore`, `docker-compose.yml`, `README.md`, `backend\...`, `frontend\...`
   - It does NOT touch your `HealthController.java` or `BackendApplication.java`.
3. In `frontend\index.html` change `<title>` to `Breathing Room`.
4. Delete the old placeholder files only if they still exist and are no longer used:
   nothing else needs deleting - every page file with the same name is replaced.
5. `pom.xml` is unchanged (it already has Web, JPA, PostgreSQL, Validation).
6. Follow README.md "Run it" to test, then commit.
