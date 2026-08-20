<#
  TeLTP task runner. Run from the Teltp folder:  .\run.ps1 <command>

  Commands:
    start        Build (if needed) and start the whole stack in the background
    up           Build and run in the foreground (streams logs; Ctrl+C to stop)
    test [class] Run backend unit tests in a Maven container (all, or one class)
    stop         Stop and remove the containers (keeps the database volume)
    reset        Stop and DELETE all data (database volume too), then you can start fresh
    logs         Follow logs from all services
    logs backend Follow logs from one service (backend | frontend | db)
    status       Show container status
    admin <user> Grant the ADMIN role to an existing username (log out/in after)
    help         Show this help
#>
param(
  [string]$Command = "help",
  [string]$Arg
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

function Assert-Docker {
  try { docker info *> $null } catch {
    Write-Host "Docker does not appear to be running. Start Docker Desktop and try again." -ForegroundColor Red
    exit 1
  }
}

function Get-EnvValue([string]$key, [string]$fallback) {
  if (Test-Path ".env") {
    $line = (Get-Content ".env" | Where-Object { $_ -match "^\s*$key\s*=" } | Select-Object -First 1)
    if ($line) { return ($line -split "=", 2)[1].Trim() }
  }
  return $fallback
}

switch ($Command.ToLower()) {
  "start" {
    Assert-Docker
    docker compose up --build -d
    docker compose ps
    Write-Host ""
    Write-Host "App:      http://localhost:9090" -ForegroundColor Green
    Write-Host "Swagger:  http://localhost:9090/api/swagger-ui.html" -ForegroundColor Green
    Write-Host "Logs:     .\run.ps1 logs        Stop: .\run.ps1 stop"
  }
  "up" {
    Assert-Docker
    docker compose up --build
  }
  "test" {
    Assert-Docker
    $mvnArgs = @("test")
    if ($Arg) { $mvnArgs = @("-Dtest=$Arg", "test") }   # e.g. .\run.ps1 test AssessmentServiceTest
    Write-Host "Running backend tests in a Maven container (deps cached in the 'teltp-m2' volume)..." -ForegroundColor Cyan
    docker run --rm -v "${PSScriptRoot}\teltp-backend:/app" -w /app -v teltp-m2:/root/.m2 maven:3.9-eclipse-temurin-21 mvn @mvnArgs
  }
  "stop" {
    docker compose down
    Write-Host "Stopped. Data is preserved. Start again with: .\run.ps1 start"
  }
  "reset" {
    Write-Host "This DELETES the database volume and all data. Continue? (y/N): " -NoNewline -ForegroundColor Yellow
    if ((Read-Host) -eq "y") { docker compose down -v; Write-Host "Wiped. Next start rebuilds from scratch." }
    else { Write-Host "Cancelled." }
  }
  "logs" {
    if ($Arg) { docker compose logs -f $Arg } else { docker compose logs -f }
  }
  "status" { docker compose ps }
  "admin" {
    if (-not $Arg) { Write-Host "Usage: .\run.ps1 admin <username>" -ForegroundColor Yellow; break }
    Assert-Docker
    $u  = Get-EnvValue "DB_USER" "teltp"
    $p  = Get-EnvValue "DB_PASSWORD" "teltp"
    $db = Get-EnvValue "DB_NAME" "teltp"
    $sql = "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='$Arg' AND r.name='ADMIN' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id);"
    docker compose exec db mysql -u$u -p$p $db -e $sql
    Write-Host "Granted ADMIN to '$Arg' (if the user exists). Log out and back in to refresh the token." -ForegroundColor Green
  }
  default {
    Write-Host "TeLTP task runner" -ForegroundColor Cyan
    Write-Host "  .\run.ps1 start          Build + start in background"
    Write-Host "  .\run.ps1 up             Build + run in foreground (Ctrl+C stops)"
    Write-Host "  .\run.ps1 test [class]   Run backend unit tests (optionally one test class)"
    Write-Host "  .\run.ps1 stop           Stop (keeps data)"
    Write-Host "  .\run.ps1 reset          Stop + delete all data"
    Write-Host "  .\run.ps1 logs [svc]     Follow logs (svc = backend|frontend|db)"
    Write-Host "  .\run.ps1 status         Container status"
    Write-Host "  .\run.ps1 admin <user>   Grant ADMIN to a username"
  }
}
