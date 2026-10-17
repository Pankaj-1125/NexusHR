# NexusHR Recruitment & Leave Workflow Upgrade

## Manager Leave Management
Manager now has the same Leave Management page controls as Admin/HR for operational leave work:
- create a leave request for any employee
- review pending requests
- approve or reject requests
- cancel eligible requests
- view balances and validation warnings

Backend ownership rules already treat ADMIN, HR and MANAGER as privileged leave operators; the frontend now exposes the create-for-employee action to MANAGER as well.

## Public Careers Portal
External candidates do not need a NexusHR account.

Public URL:

`http://localhost/careers`

A job-specific shareable URL is:

`http://localhost/careers?job=<JOB_ID>`

Admin/HR/Manager can copy this link from Recruitment. The login screen also has an "Explore Careers / Apply as External Candidate" button.

The public form collects:
- job
- full name
- email
- phone
- current city
- qualification
- experience
- expected salary
- notice period
- skills
- LinkedIn
- portfolio/GitHub
- resume URL
- cover letter

Every external application receives an application reference such as `NXR-APP-2026-000123`.
Candidates can track status using the reference + email on the same public Careers page.

## Two-stage shortlist approval
Pipeline:

`APPLIED -> SCREENING -> MANAGER_SHORTLISTED -> ADMIN_APPROVED -> INTERVIEW -> OFFER -> HIRED`

Rejection can occur during the workflow.

- MANAGER can mark a candidate `MANAGER_SHORTLISTED`.
- ADMIN must provide the final shortlist approval (`ADMIN_APPROVED`).
- Admin/HR/Manager can view the candidate pipeline and candidate details.
- Workflow stores who performed manager shortlist and final admin approval, with timestamps.
- Candidate email updates are attempted when SMTP is enabled; workflow continues when email is disabled/unavailable.

## Public API
- `GET /api/public/jobs`
- `GET /api/public/jobs/{id}`
- `POST /api/public/applications`
- `GET /api/public/applications/status?reference=...&email=...`

Authenticated recruitment management remains under `/api/jobs/**` and `/api/applications/**`.

## Database
Flyway migration `V14__public_recruitment_workflow.sql` adds candidate profile, source, tracking-reference and two-stage approval fields and expands application statuses.

Do not delete the PostgreSQL Docker volume. Flyway upgrades the existing database automatically.
