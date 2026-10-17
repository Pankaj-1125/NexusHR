# NexusHR Payroll & Performance Completion

This revision completes the two remaining core HR areas requested for the internship project: Payroll Processing and Performance Management.

## Payroll

Implemented:
- Manual payroll create/edit with duplicate employee-month protection.
- Monthly batch payroll generation for active employees.
- Configurable payroll policy values for allowance, provident fund, professional tax and tax/TDS estimate.
- Earnings: basic salary, allowances, bonus, overtime pay.
- Deductions: general deductions, provident fund, professional tax, additional deductions, tax/TDS.
- Automatic gross, tax and net salary calculation.
- Workflow: DRAFT -> PROCESSED -> PAID.
- Process timestamp, payment date, payment reference and paid timestamp.
- Paid payroll locking to prevent silent edits after payment.
- Payroll summary API and dashboard cards.
- Search, month and status filters.
- Server-generated PDF payslip with expanded earnings/deductions and payment information.
- Email payslip with PDF attachment.
- Employee in-app/email notifications when payroll is processed or paid.
- Employee self-service payroll history, detailed deductions and secure PDF download.

Important: configured tax/PF/professional-tax values are a demo/configurable policy engine. They are not a substitute for a payroll compliance provider or statutory filing validation.

## Performance

Implemented:
- Performance review creation/editing.
- Review cycle and review type.
- Workflow: DRAFT -> SELF_REVIEW -> SUBMITTED -> MANAGER_REVIEW -> COMPLETED.
- Rating, competency score, goal score and weighted final score.
- Strengths, improvement areas, manager comments and employee comments.
- Goal management with title, description, KPI/success metric, dates, weight, progress and status.
- Goal lifecycle: DRAFT / ACTIVE / COMPLETED / CANCELLED.
- Employee self-service goal progress update.
- 360-degree feedback: SELF / MANAGER / PEER / SUBORDINATE.
- Employee self-feedback submission.
- Performance analytics: average rating, final score, goal progress, overdue goals, feedback volume and 360 average.
- Review trend and goal-health dashboard views.
- Employee notifications/email when a review moves through workflow stages.
- Role protection for management endpoints.

## Database migrations

- V12__advanced_payroll.sql
- V13__performance_goal_lifecycle.sql

Flyway applies these migrations automatically. Do not delete the existing PostgreSQL volume.

## Verification

From the NexusHR root on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\verify.ps1
```

Then, with Docker Desktop running:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1
```

Open `http://localhost`.

## Recommended functional checks

### Payroll
1. Login as ADMIN or HR.
2. Open Payroll Management.
3. Run a monthly batch for a month with active employees.
4. Open a DRAFT record and verify earnings/deductions.
5. Process the record and confirm the employee receives a notification.
6. Mark it paid with a payment reference.
7. Download and email the payslip.
8. Login as that employee and verify My Payroll.

### Performance
1. Login as ADMIN, HR or MANAGER.
2. Create an employee goal and activate it.
3. Create a review cycle.
4. Advance the review through SELF_REVIEW, SUBMITTED, MANAGER_REVIEW and COMPLETED.
5. Add MANAGER, PEER and SUBORDINATE feedback.
6. Login as the employee, update goal progress and submit SELF feedback.
7. Return to management Performance and verify analytics and weighted score.
