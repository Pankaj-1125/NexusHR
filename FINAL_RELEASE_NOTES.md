# NexusHR Final Release Notes

This release is prepared for Docker-based deployment and includes a frontend quality pass plus employee self-service attendance hardening.

## Key improvements

- Crisp Windows/Chrome typography using the native Segoe UI stack.
- Removed full-page blur/backdrop effects that could make text look soft.
- Larger, consistent typography across dashboard, tables, forms, sidebar, payroll and leave screens.
- Consolidated employee action-button styling and removed duplicate generated Delete labels.
- Cleaner payroll action layout for processed/unpaid rows.
- Improved leave-management readability.
- Employee self-service attendance now surfaces attendance-specific errors and immediately reflects successful check-in/check-out responses.
- Legacy EMPLOYEE and MANAGER accounts without an Employee link are repaired on first self-service access, while ADMIN/HR accounts are never auto-converted.
- Backend Docker image now builds the Spring Boot JAR from source in a Maven build stage; a prebuilt local JAR is no longer required.
- Nginx includes compression, safe static asset caching, SPA no-cache handling, WebSocket proxying and common security headers.
- `deploy.ps1` now requires only Docker Desktop for deployment.

## Deployment

1. Copy `.env.example` to `.env` if needed.
2. Replace all `CHANGE_ME` values with strong production values.
3. Set `CORS_ALLOWED_ORIGINS` to your real frontend origin/domain.
4. Start Docker Desktop.
5. Run `powershell -ExecutionPolicy Bypass -File .\deploy.ps1`.
6. Verify `docker compose ps` and `http://localhost/api/health`.

## Production checklist

- Use a strong unique `POSTGRES_PASSWORD`.
- Use a long random `JWT_SECRET` (64+ characters recommended).
- Disable demo-user seeding after first controlled setup.
- Configure SMTP only with an app-specific credential/secret.
- Put the application behind HTTPS in production.
- Back up the PostgreSQL named volume before upgrades.
- Test Admin, Manager and Employee accounts separately after deployment.
