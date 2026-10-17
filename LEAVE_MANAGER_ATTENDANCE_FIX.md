# NexusHR Leave Management + Manager My Attendance Fix

## Manager My Attendance

Older demo databases could contain a `MANAGER` login without a linked `employees` row. `/api/me/**` therefore failed with:

`No employee profile is linked to user: manager`

The self-service resolver now:

1. Uses an existing `employees.user_id` link when present.
2. Repairs an older unlinked employee automatically when the manager user's recovery e-mail matches an employee e-mail.
3. For a legacy MANAGER account only, creates one approved/active Manager employee profile when no employee can be matched.
4. Loads `/api/me` before `/api/me/attendance` in the Manager Attendance UI to avoid concurrent profile provisioning.

Admin/HR users are not auto-created as employee profiles.

## Leave Management

The frontend previously offered leave types that the backend rejected. Backend and UI are now aligned for:

- CASUAL
- SICK
- ANNUAL
- MATERNITY
- PATERNITY
- UNPAID
- OTHER

Entitlements are configuration-driven. The default project/demo values are not a substitute for statutory payroll/leave policy review.

Management workflow improvements:

- ADMIN/HR can create a request on behalf of an employee.
- ADMIN/HR/MANAGER can review pending requests.
- Review modal loads the employee's leave balance before approval.
- Entitlement, approved days, pending days and remaining days are visible.
- Approve is disabled when a request exceeds the remaining balance.
- A clear explanation shows how many days exceed the balance.
- Reject remains available for invalid/over-balance requests.
- Success messages are shown after approval/rejection.

No database reset or Docker volume deletion is required.
