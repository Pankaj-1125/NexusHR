# NexusHR – AI-Enabled Enterprise HR & Workforce Intelligence Platform

NexusHR is a Java 21 + Spring Boot 3.3 + React 19 + PostgreSQL HR platform aligned with the supplied Zidio Java Full-Stack project brief.

## Implemented functional coverage

- **Employee lifecycle:** employee CRUD, approval workflow, offboarding/reactivation, employee documents, first-time OTP username/password setup, permanent deletion with linked-data cleanup.
- **Attendance & leave:** check-in/check-out, daily/range views, leave requests, approvals/rejections and balances.
- **Payroll:** payroll records, calculations, PDF payslips and reporting.
- **Performance:** reviews, ratings, goals, strengths, areas for improvement and 360 feedback.
- **AI Workforce Insights:** explainable attrition-risk scoring, engagement score, attendance rate, leave signals, performance signals, skill-gap indicators, data-confidence score, priority classification, workforce summary and HR recommendations.
- **Admin/manager dashboards & reports:** workforce metrics, PDF/Excel exports and audit logs.
- **Notification & Communication Center:** individual messages, department broadcast, company-wide broadcast, unread/read tracking, real-time WebSocket delivery, optional SMTP email, SMS simulation, delivery summaries and notification deletion.
- **Security:** JWT access/refresh tokens, RBAC, method authorization, audit trail and password-reset OTP flow.

## Important note about “AI”

The default runnable build uses an **explainable analytical workforce model** based on live HR signals. It does not require a paid/private AI key, which keeps the demo reproducible. The architecture can later add Spring AI/OpenAI/Hugging Face as an optional narrative/model provider without making the core project dependent on external credentials.

## Permanent employee deletion fix

The runtime deletion flow now follows the PostgreSQL-safe sequence validated against the schema:

1. retain an audit record;
2. remove username-based reset/refresh tokens;
3. remove notification rows (V10 also enforces cascade integrity);
4. delete the employee, allowing V9 cascades to remove attendance/leave/payroll/performance/documents;
5. delete the linked login user.

The frontend API client now displays backend error text/JSON instead of hiding useful errors behind only `Request failed with status 400`.

## Fast Windows deployment

Requirements: Java 21, Maven, Docker Desktop.

```powershell
Set-Location "C:\Users\PANKAJ\Desktop\NexusHR"
Copy-Item .env.example .env
notepad .env
.\deploy.ps1
```

Set strong values for `POSTGRES_PASSWORD` and `JWT_SECRET`. If you already have a working `.env`, copy it into this project instead of creating a new one.

Open `http://localhost` after the containers are healthy.

## Full verification

```powershell
.\verify.ps1
```

This runs backend tests, creates the backend JAR, installs/builds the frontend, and validates Docker Compose.

## Notification channels

- In-app: always available.
- WebSocket: real-time delivery to logged-in employee accounts.
- Email: set `MAIL_ENABLED=true` and valid SMTP values in `.env`.
- SMS: simulation mode by default (`SMS_SIMULATION_ENABLED=true`), so the demo remains runnable without a paid SMS provider.

## Security / repository hygiene

Do not commit `.env`, passwords, JWT secrets, SMTP app passwords, build output, log files, IDE folders or backup source files. The cleaned submission package contains `.env.example` rather than personal secrets.

## Project brief scope

The supplied brief also describes production targets such as Kubernetes/Helm, Prometheus/Grafana/Sentry, cloud EKS, 10k+ concurrency, 99.95% availability and zero-downtime deployment. Those are **production roadmap/infrastructure targets**, not claims made by this local Docker Compose build. The core F-01 through F-07 functional modules are represented in the runnable application.

## AI Workforce Insights v2.0

NexusHR includes an explainable workforce-intelligence engine for attrition risk, engagement, burnout, growth potential and skill-gap analysis. HR/Manager/Admin users can access employee-level and department-level intelligence, recommendations, risk distributions, confidence indicators and CSV export. See `AI_WORKFORCE_INSIGHTS.md` for details.

## Communication & Recruitment Enhancements

NexusHR communication now follows an **email-first** model for detailed business communication, with in-app messages for traceability and short SMS alerts for urgent attention.

- ADMIN / HR / MANAGER messages include a subject, sender username and sender role.
- Employees can see exactly who sent each in-app notification.
- Leave submissions can email approval reviewers; approval/rejection decisions notify and email the employee.
- Recruitment can notify every active employee and attach a generated NexusHR job-alert PDF.
- SMS can use Twilio when configured and otherwise falls back to simulation.
- Signed-in usernames are visible in management and employee self-service headers.

See `COMMUNICATION_AND_RECRUITMENT.md` for configuration and workflow details.

## Payroll & Performance completion

Advanced payroll batch processing, payment workflow, payslip/email delivery, performance goals, review lifecycle and 360-degree feedback are documented in `PAYROLL_PERFORMANCE_COMPLETION.md`.
