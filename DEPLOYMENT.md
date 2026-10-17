# NexusHR Deployment

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop with Docker Compose
- Node/npm only if you want to run the frontend outside Docker

## 1. Configure environment

```powershell
Set-Location "C:\Users\PANKAJ\Desktop\NexusHR"
Copy-Item .env.example .env
notepad .env
```

Replace all `CHANGE_ME` values. For an existing NexusHR installation, you can reuse your current `.env` instead.

## 2. Verify before deployment

```powershell
.\verify.ps1
```

This runs backend tests/package, frontend install/build and Docker Compose validation.

## 3. Deploy

```powershell
.\deploy.ps1
```

The deployment script always rebuilds the backend JAR before Docker starts, so the container cannot accidentally run an older JAR.

## 4. Check services

```powershell
docker compose ps
docker compose logs backend --tail=100
```

Expected application URL:

```text
http://localhost
```

## Database migrations

Flyway runs automatically. This revision includes:

- V9: employee-linked operational tables use `ON DELETE CASCADE`.
- V10: notification rows are linked to employees with `ON DELETE CASCADE` after legacy orphan cleanup.

Do not use `docker compose down -v` on an existing environment unless you intentionally want to delete the PostgreSQL volume.

## Notification email

To enable SMTP email, set these in `.env`:

```text
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_account
MAIL_PASSWORD=your_app_password
MAIL_FROM=your_account
```

SMS is intentionally simulation-only unless you later integrate an external provider.

## Troubleshooting

If a frontend API error occurs, this revision surfaces the backend message instead of only the HTTP status. For backend diagnostics:

```powershell
docker compose logs backend --tail=200
```

For a clean source verification without deleting your database:

```powershell
.\verify.ps1
```
