# Checks backend\.env and your Windows settings BEFORE the backend starts, so a wrong password
# is caught here with a plain message instead of a long Hibernate error.
# It never prints passwords or keys: only whether they are set and how long they are.
param([switch]$SkipDbTest)

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root "backend\.env"
$script:problems = 0
function Bad($msg)  { Write-Host "  [PROBLEM] $msg" -ForegroundColor Red; $script:problems++ }
function Warn($msg) { Write-Host "  [warning] $msg" -ForegroundColor Yellow }
function Ok($msg)   { Write-Host "  [ok]      $msg" -ForegroundColor Green }

Write-Host "1. Reading backend\.env"
if (-not (Test-Path $envFile)) {
    Bad "backend\.env was not found. Copy backend\.env.example to backend\.env and fill it in."
    exit 1
}
$vars = @{}
foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*#' -or $line.IndexOf('=') -lt 1) { continue }
    $i = $line.IndexOf('=')
    $vars[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1)
}
foreach ($name in "DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET") {
    if (-not $vars.ContainsKey($name) -or [string]::IsNullOrEmpty($vars[$name])) { Bad "$name is missing or empty in backend\.env"; continue }
    $v = $vars[$name]
    if ($v -ne $v.Trim()) { Bad "$name has a space at the start or end. Remove it." }
    if ($v -match '^["'']|["'']$') { Bad "$name is wrapped in quotes. Remove the quotes." }
    if ($name -eq "DB_PASSWORD" -and $v -match '[\\${}]') { Warn "DB_PASSWORD contains one of \ $ { } which can be changed when the file is read. Use letters, digits, - and _ only." }
    if ($name -eq "JWT_SECRET" -and $v.Length -lt 32) { Bad "JWT_SECRET must be at least 32 characters (it is $($v.Length))." }
}
if ($vars.ContainsKey("DB_URL")) {
    Write-Host "     DB_URL = $($vars['DB_URL'])   user = $($vars['DB_USERNAME'])   password length = $($vars['DB_PASSWORD'].Length)"
}

Write-Host "2. Looking for Windows settings that would override .env"
$overrides = $false
foreach ($name in "DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "OPENAI_API_KEY") {
    foreach ($scope in "Process", "User", "Machine") {
        $val = [Environment]::GetEnvironmentVariable($name, $scope)
        if ($val) {
            if ($vars.ContainsKey($name) -and ($vars[$name] -ceq $val)) {
                Warn "$name is also set as a $scope environment variable (same value as .env, but delete it so there is one source)."
            } else {
                Bad "$name is set as a $scope environment variable and is DIFFERENT from backend\.env. The environment variable wins, so .env is ignored."
            }
            $overrides = $true
        }
    }
}
if ($overrides) {
    Write-Host "     To clear one for this window:   Remove-Item Env:DB_PASSWORD"
    Write-Host "     To clear one for your Windows user: [Environment]::SetEnvironmentVariable('DB_PASSWORD', `$null, 'User')"
    Write-Host "     Also check IntelliJ: Run > Edit Configurations > BackendApplication > Environment variables (delete DB_* entries)."
} else {
    Ok "No overriding environment variables."
}

Write-Host "3. Testing the database"
if ($vars.ContainsKey("DB_URL") -and ($vars["DB_URL"] -match '^jdbc:postgresql://([^:/]+):?(\d*)/(\w+)')) {
    $dbHost = $Matches[1]
    if ($Matches[2]) { $dbPort = [int]$Matches[2] } else { $dbPort = 5432 }
    $dbName = $Matches[3]
    $open = Test-NetConnection -ComputerName $dbHost -Port $dbPort -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($open) { Ok "PostgreSQL is listening on ${dbHost}:${dbPort}." } else { Bad "Nothing is listening on ${dbHost}:${dbPort}. Start PostgreSQL (or Docker) and check the port in DB_URL." }
    $psql = Get-Command psql -ErrorAction SilentlyContinue
    if ($SkipDbTest) {
        Warn "Login test skipped."
    } elseif (-not $open) {
        Warn "Login test skipped because the port is closed."
    } elseif (-not $psql) {
        Warn "The psql tool was not found, so the login could not be tested here. In DataGrip, use Test Connection with the SAME user and password as .env."
    } else {
        $env:PGPASSWORD = $vars["DB_PASSWORD"]
        $out = & psql -h $dbHost -p $dbPort -U $vars["DB_USERNAME"] -d $dbName -tA -c "select 1" 2>&1
        $code = $LASTEXITCODE
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        if ($code -eq 0) { Ok "PostgreSQL accepted the user and password from backend\.env." }
        else { Bad "PostgreSQL rejected the login: $out" }
    }
} else {
    Bad "DB_URL should look like jdbc:postgresql://localhost:5432/breathingroom"
}

Write-Host ""
if ($script:problems -gt 0) {
    Write-Host "Found $($script:problems) problem(s). Fix them, then run this again." -ForegroundColor Red
    Write-Host "If the password is the problem: set the SAME password in 3 places: (1) run ALTER USER in DataGrip (no -- in front), (2) DataGrip data source, (3) DB_PASSWORD in backend\.env."
    exit 1
}
Write-Host "All checks passed." -ForegroundColor Green
exit 0
