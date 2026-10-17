# NexusHR Recruitment, Manager Leave & Employee Dashboard Enhancement

## Manager leave
- Manager gets a **My Leave** self-service entry in the sidebar.
- Manager can raise leave only for their own linked employee account.
- Admin/HR may still create leave for employees through Leave Management.
- If the leave belongs to a MANAGER, only ADMIN can approve or reject it.
- Existing employee leave review permissions remain unchanged for non-manager employees.

## Recruitment / Job Applications
- Existing Manager shortlist and Admin direct-shortlist flows remain available.
- Interview scheduling remains available to Admin/Manager after Admin shortlist approval.
- Added **Mark interviewed** after the scheduled interview.
- New status: `INTERVIEW_COMPLETED`.
- Only after `INTERVIEW_COMPLETED`, Admin sees **Final approve**.
- Final approval opens a form for:
  - Final selection/congratulations message
  - Required document checklist
  - Other/reporting/joining instructions
- Final approval moves the application to `OFFER` and emails the candidate.
- Candidate public application tracking now displays the final selection message and documents.

## Employee dashboard
- Added **My HR Snapshot** to the employee self-service dashboard.
- Snapshot includes employee ID, department, job title, joining date, active goals and next approved leave.
- Existing dashboard attendance, leave, payroll, performance, documents, notifications and quick actions remain intact.

## Database
- Added Flyway migration `V16__post_interview_final_selection.sql`.
- Adds final selection/document fields and updates the job-application status check for `INTERVIEW_COMPLETED`.
