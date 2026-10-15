# Starts the backend apps locally, each in its own window / JVM, so one crashing or being closed
# never takes the others down. Config comes from the shared .env next to this script.
#   .\run-all.ps1                 -> core, crm, stats, promotions
#   .\run-all.ps1 core,crm        -> only those
# Stop an app by closing its window (or Ctrl+C in it).
param([string[]]$Apps = @('core', 'crm', 'stats', 'promotions'))

$root = $PSScriptRoot
$ports = @{ core = 8080; crm = 8081; stats = 8082; promotions = 8083 }

if (-not (Test-Path (Join-Path $root '.env'))) {
    Write-Warning "Missing $root\.env - copy .env.example to .env and fill in the DB / JWT_SECRET first."
    exit 1
}

foreach ($app in $Apps) {
    $dir = Join-Path $root "app-$app"
    if (-not (Test-Path $dir)) { Write-Warning "Unknown app '$app' (expected one of: $($ports.Keys -join ', '))"; continue }
    $title = "app-$app :$($ports[$app])"
    Start-Process powershell -WorkingDirectory $dir -ArgumentList @(
        '-NoExit', '-Command',
        "`$Host.UI.RawUI.WindowTitle = '$title'; .\mvnw.cmd spring-boot:run"
    )
    Write-Host "started $title"
}
Write-Host "Health: http://localhost:<port>/actuator/health   Swagger: http://localhost:<port>/swagger-ui/index.html"
