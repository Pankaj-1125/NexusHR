$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or not available in PATH."
}

docker info *> $null
if ($LASTEXITCODE -ne 0) { throw "Docker Desktop engine is not running." }

Write-Host "[1/4] Docker Compose configuration" -ForegroundColor Cyan
docker compose config --quiet
if ($LASTEXITCODE -ne 0) { throw "docker-compose.yml is invalid." }

Write-Host "[2/4] Clean source build" -ForegroundColor Cyan
docker compose build --no-cache
if ($LASTEXITCODE -ne 0) { throw "Docker build failed." }

Write-Host "[3/4] Start services" -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) { throw "Docker Compose start failed." }

Start-Sleep -Seconds 20

docker compose ps

Write-Host "[4/4] Health checks" -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "http://localhost/api/health" -TimeoutSec 10
if ($health.status -ne "UP") { throw "Backend health check did not return UP." }

$frontend = Invoke-WebRequest -Uri "http://localhost" -UseBasicParsing -TimeoutSec 10
if ($frontend.StatusCode -ne 200) { throw "Frontend did not return HTTP 200." }

Write-Host "NexusHR Docker verification passed." -ForegroundColor Green
