# NexusHR V14 - Authentication / 403 Fix

Changes:

- Explicitly forwards the `Authorization` header from Nginx to Spring Boot.
- Frontend API client now falls back to `nexushr-token` if the token is not present in the saved user object.
- Spring Security role construction safely normalizes both `ADMIN` and `ROLE_ADMIN` database values to `ROLE_ADMIN`.
- Security now returns a clear `401` for missing/invalid authentication and a clear `403` only for insufficient role permissions.

This change does not modify employee, attendance, leave, payroll, recruitment, or dashboard data.
