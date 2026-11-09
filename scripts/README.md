# Start scripts (Windows PowerShell)

From the repo root (`breakthrough_app`):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
```

| Script | What it does |
|---|---|
| `check-env.ps1` | Checks `backend\.env` (missing values, quotes, spaces), finds Windows settings that override it, tests that PostgreSQL is listening, and tests the login if `psql` is installed. Never prints secrets. |
| `start-backend.ps1` | Runs `check-env.ps1`, then starts the backend from the `backend` folder and saves `backend\run.log`. |
| `start-frontend.ps1` | Installs packages if needed and starts the React app from the `frontend` folder. |
| `start-all.ps1` | Opens the backend and frontend in two windows. |

Running a command in the wrong folder (for example `npm run dev` inside `backend`) is one of the most common mistakes. These scripts always use the right folder.
