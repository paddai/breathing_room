# Starts the backend the safe way: checks your settings first, then runs it from the right folder.
& (Join-Path $PSScriptRoot "check-env.ps1")
if ($LASTEXITCODE -ne 0) { exit 1 }
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root "backend")
Write-Host "Starting the backend. Wait for 'Started BackendApplication'. The log is also saved in backend\run.log" -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run 2>&1 | Tee-Object -FilePath run.log
