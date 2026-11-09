# Opens the backend and the frontend in two windows.
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "start-backend.ps1")
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "start-frontend.ps1")
Write-Host "Started two windows. Open http://localhost:5173 when the backend says 'Started BackendApplication'."
