# NexusHR Text Clarity + Employee Attendance Fix

## UI clarity
- Removed backdrop blur from topbar, panels, modals, leave overlays, login shell/cards, payroll overlays, and employee workspace overlays.
- Disabled remaining content-level backdrop/filter blur through the final ProfessionalUI override.
- Switched application text to a crisp Windows-friendly Segoe UI/Arial stack.
- Increased opacity of main surfaces so text is rendered over solid white cards instead of translucent glass.
- Preserved gradients and motion without blurring content.

## Employee attendance
- `/api/me/attendance`, `/api/me/attendance/check-in`, and `/api/me/attendance/check-out` remain authenticated self-service endpoints.
- Employee account resolution now checks the existing user link first, then recovery email case-insensitively, then an email-form username case-insensitively.
- Existing employee records are linked to the authenticated account before any fallback employee is provisioned.
- Manager/Employee legacy accounts without an Employee record can still be provisioned automatically.
- EmployeeWorkspace refreshes attendance after successful check-in/check-out and surfaces attendance-specific errors.
