# Employee Dashboard Enhancement

## Attendance fixes
- Working duration is rounded to whole minutes and rendered only as `Hh MMm`; floating-point minute values are eliminated.
- Backend AttendanceDTO now exposes `workingMinutes` and keeps `workingHours` rounded to two decimal places for compatibility.
- Today attendance redesigned with a clear check-in/check-out workflow, live status panel, timeline, completed/open state, and final worked duration.
- Current-month totals and averages use minute-based arithmetic, preventing decimal drift.
- Attendance table shows Date, Day, Check in, Check out, Working hours, Status, and Remarks.
- Filters remain available for date range and status.

## Employee overview
- Added cards for today check-in, today check-out, pending leave, and tracked attendance this month.
- Existing cards for attendance, leave balance, payroll, performance, notifications, documents, worked time, and payroll history remain.

## Other self-service dashboards
- Leave now includes summary cards for remaining balance, pending, approved, and rejected/cancelled requests.
- Notifications now includes total, unread, and read summary cards.
- Payroll and Performance existing summary cards are preserved.

## Verification
Run `verify.ps1` on Windows from the project root. It runs backend tests/package, frontend production build, and Docker Compose validation.
