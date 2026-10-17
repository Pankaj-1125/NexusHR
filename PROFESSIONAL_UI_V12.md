# NexusHR Professional UI v12

This version keeps the existing HR workflows and upgrades the frontend presentation layer.

## Main improvements
- Removed the old Vite starter `#root` width constraint so NexusHR uses the full screen.
- Added a subtle animated gradient background behind authenticated dashboards.
- Added reduced-motion support for accessibility.
- Standardized dashboard spacing, radii, shadows, typography, controls and table presentation.
- Changed metric grids to responsive auto-fit card layouts to avoid cramped cards.
- Refined sidebar and sticky topbar presentation.
- Enhanced Admin Attendance with a full-width header, responsive metric cards, filter surface and attendance table surface.
- Enhanced Employee Attendance with a prominent workday command center, status panel, responsive metrics and clearer timeline cards.
- Enhanced Employee Workspace card layout and dashboard spacing.
- Enhanced Leave dashboard metric-card arrangement.
- Added responsive layouts for desktop, tablet and mobile.

## Main frontend files changed
- `frontend/src/index.css`
- `frontend/src/App.jsx`
- `frontend/src/ProfessionalUI.css` (new)

No backend API, security, database or workflow behavior was intentionally changed by this UI update.
