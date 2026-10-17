# NexusHR V13 - Backend startup fix + enhanced login

## Backend startup fix
- Added a safe local-development JWT fallback in `application.properties`.
- Added the same fallback to `docker-compose.yml`.
- A real `JWT_SECRET` from `.env` still overrides the fallback.
- This prevents Spring Boot from failing with `Could not resolve placeholder 'JWT_SECRET'` when the environment variable is not loaded.

## Login UI enhancement
- Reused the existing `/api/auth/login`, `/api/auth/forgot-password`, and `/api/auth/reset-password` APIs.
- Replaced the inline-style login card with a responsive enterprise split-screen layout.
- Added subtle animated background glows/grid, larger typography, professional spacing, glass effects, feature cards, security indicators, and responsive mobile layout.
- Kept Remember Username, Show/Hide Password, Forgot Password, OTP reset, first-time username selection, and Careers portal access.
- Added password rule indicators during reset.
- Added reduced-motion support.

## Verification
- `frontend/src/Login.jsx` parses successfully with the project's ESLint setup.
