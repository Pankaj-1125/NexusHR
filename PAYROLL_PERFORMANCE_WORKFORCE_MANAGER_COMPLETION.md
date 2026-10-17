# NexusHR – Payroll, Performance, Workforce Intelligence & Manager Attendance Update

## Restored previous features

### Payroll
- Create and edit payroll for ADMIN/HR.
- Monthly batch payroll.
- Automatic gross, tax and net calculation.
- DRAFT → PROCESSED → PAID workflow.
- Payment reference/date tracking.
- PDF payslip download.
- Payslip email support.
- Payroll employee notifications.
- Search, month filter and status filter.
- Summary cards for gross, net, tax, deductions and status counts.

### Performance
- Performance reviews and review lifecycle.
- Goal management.
- 360-degree feedback.
- Analytics tab.
- Review scorecards and trends.
- Employee filters and status filters.
- Employee self-service performance/goal support.

### Workforce Intelligence
- Attrition risk.
- Engagement scoring.
- Burnout risk.
- Growth potential.
- Skill-gap scoring.
- Attendance/performance signals.
- Employee and department intelligence.
- Risk factors, strengths and recommendations.
- CSV export.

## Critical fix
Payroll.jsx and Performance.jsx now normalize roles such as `ROLE_ADMIN`, `ROLE_HR`, and `ROLE_MANAGER` before checking permissions. This restores management controls that were hidden even while the user was visibly logged in as Administrator.

## New payroll improvements
- Clear workflow explanation card.
- Awaiting-process and awaiting-payment counters.
- Access-level indicator (Manage / Read only).
- Existing batch/create/process/paid/PDF/email functions preserved.

## New performance improvements
- Clear review lifecycle explanation.
- More KPI cards: active goals and overdue goals.
- Work queue summary for incomplete reviews, active goals and feedback.
- Useful empty state when no reviews exist instead of a blank table.
- Existing review/goal/360° creation forms preserved.

## Workforce Intelligence improvements
- Workforce Health score.
- Average Engagement.
- Average Attendance.
- Average Skill Gap.
- Average Performance Rating.
- Data coverage for attendance and performance history.
- Average confidence indicator.
- Plain-language explanation of every major signal and how green/amber/red should be interpreted.
- Existing employee detail, department intelligence, distributions and recommendations preserved.

## Manager-only attendance
A new `My Attendance` page appears only for MANAGER users. It includes:
- Manager's own linked employee profile and username.
- Check In.
- Check Out.
- Today's status.
- Today's check-in/check-out.
- Today's worked time.
- Present days this month.
- Total worked hours this month.
- Average workday.
- Date-filtered personal attendance history.

The manager self-attendance page uses `/api/me/**`, so it does not expose arbitrary employee attendance through the personal view. A manager account must be linked to an Employee record for personal attendance to work.

## Security/CORS
- `/api/me/**` explicitly requires authentication.
- PATCH is allowed by CORS for goal-progress/self-service partial updates.
