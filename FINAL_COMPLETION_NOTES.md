# NexusHR Final Completion Pass

This release includes the remaining stabilization work requested before deployment.

## Completed in this pass

- Employee Performance summary cards now use a responsive 3-column layout on normal desktops, 6 columns only on wide screens, and safe wrapping for long values.
- Performance workflow statuses are humanized in employee self-service (`MANAGER_REVIEW` -> `Manager Review`).
- Remaining visible mojibake in the main application shell was corrected.
- Manager leave policy was tightened: managers can still review eligible employee leave, but cannot cancel another employee's managed leave from the team Leave Management screen.
- Backend cancellation authorization now permits Admin/HR to cancel managed requests while Manager/Employee accounts can only cancel their own leave.
- Employee self-service attendance retains the repaired account-to-employee linking and check-in/check-out refresh flow.
- No-blur text clarity rules remain active.
- Personal SMTP/database/JWT credentials from the working copy were removed. A fresh generated local `.env` is included and SMTP is disabled by default.

## Production checklist

1. Change all generated secrets and first-run passwords.
2. Set `CORS_ALLOWED_ORIGINS` to your HTTPS domain.
3. Configure SMTP only with deployment-specific credentials.
4. Set `SEED_DEFAULT_USERS=false` after initial account creation.
5. Terminate TLS/HTTPS at your reverse proxy or cloud load balancer.
6. Run `docker compose build --no-cache` and `docker compose up -d`.
7. Verify Admin, Manager, and Employee end-to-end flows before public launch.
