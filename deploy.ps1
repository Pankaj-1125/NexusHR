$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or not available in PATH. Install/start Docker Desktop first."
}

try {
    docker info *> $null
} catch {
    throw "Docker Desktop engine is not running. Start Docker Desktop and try again."
}

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example." -ForegroundColor Yellow
    Write-Host "Edit .env and replace CHANGE_ME values, then run .\deploy.ps1 again." -ForegroundColor Yellow
    exit 1
}

$envText = Get-Content ".env" -Raw
if ($envText -match "CHANGE_ME") {
    throw "Edit .env and replace all CHANGE_ME values before deployment."
}

Write-Host "Validating Docker Compose..." -ForegroundColor Cyan
docker compose config --quiet
if ($LASTEXITCODE -ne 0) { throw "docker-compose.yml is invalid." }

Write-Host "Building backend + frontend from source..." -ForegroundColor Cyan
docker compose build --no-cache
if ($LASTEXITCODE -ne 0) { throw "Docker build failed." }

Write-Host "Starting NexusHR..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) { throw "Docker Compose start failed." }

Write-Host "Waiting for services..." -ForegroundColor Cyan
Start-Sleep -Seconds 20

docker compose ps

try {
    $health = Invoke-RestMethod -Uri "http://localhost/api/health" -TimeoutSec 10
    Write-Host "Backend health: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "Containers started, but the health endpoint is not ready yet. Run: docker compose logs backend --tail=100" -ForegroundColor Yellow
}

Write-Host "NexusHR deployment command completed. Open http://localhost (or your configured APP_PORT)." -ForegroundColor Green
