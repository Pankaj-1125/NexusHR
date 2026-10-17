# Recruitment Interview Enhancement

Implemented additions:

- Admin can directly shortlist candidates from APPLIED or SCREENING.
- Existing Manager shortlist -> Admin final approval workflow remains available.
- Only ADMIN or MANAGER can schedule/reschedule interviews after Admin approval.
- Interview schedule stores date, time, location, short message, sender, role and status.
- Scheduling automatically moves the application to INTERVIEW.
- Candidate receives interview details by email when email service is configured.
- Public Careers status tracking now displays latest interview date, time, location, message and status.
- Interview schedule supports SCHEDULED, RESCHEDULED, COMPLETED and CANCELLED states.
- New Flyway migration: V15__interview_scheduling.sql.

Main new APIs:

- PUT /api/applications/{id}/admin-direct-shortlist
- POST /api/interviews/application/{applicationId}
- PUT /api/interviews/application/{applicationId}
- GET /api/interviews/application/{applicationId}
- PUT /api/interviews/{interviewId}/cancel

Security:

- Admin direct shortlist: ADMIN only.
- Interview schedule/reschedule/cancel: ADMIN or MANAGER.
- Interview scheduling is rejected unless the application already has Admin approval (or is already in INTERVIEW for rescheduling).
