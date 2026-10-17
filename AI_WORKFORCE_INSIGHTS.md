# NexusHR AI Workforce Insights — Completed Module

## Scope

The AI Workforce Insights module is implemented as an explainable local decision-support engine. It does not require a paid external AI key and does not make automated employment decisions.

## Employee-level intelligence

- Predicted attrition risk score and LOW/MEDIUM/HIGH level
- Retention score
- Engagement score and level
- Burnout risk score and level
- Growth-potential score and level
- Skill-gap severity score and level
- Attendance rate, absence count and average working hours
- Pending and approved leave signals
- Average performance rating and performance trend
- Data-confidence score and data-quality label
- HR action priority: STABLE / WATCH / URGENT
- Risk factors, skill-gap indicators, strengths and recommended HR actions
- Human-readable insight summary

## Workforce-level intelligence

- Workforce size
- High/medium/low attrition-risk distribution
- Low-engagement count
- High-burnout count
- High-potential count
- Urgent-action count
- Average risk, engagement, burnout, potential and confidence
- Department workforce distribution
- Risk-signal frequency

## Department intelligence

Each department includes:

- employee count
- average attrition risk
- average engagement
- average attendance
- average burnout risk
- high-risk employee count
- low-engagement count
- high-potential count
- urgent-action count
- department health: HEALTHY / WATCH / NEEDS_ATTENTION

## API endpoints

- `GET /api/workforce-intelligence/employees`
- `GET /api/workforce-intelligence/employee/{employeeId}`
- `GET /api/workforce-intelligence/summary`
- `GET /api/workforce-intelligence/departments`

Access remains restricted to ADMIN, HR and MANAGER roles.

## Frontend

The AI Workforce Insights screen now provides:

- executive KPI cards
- employee and department intelligence views
- search, department, risk and priority filters
- risk/engagement/burnout/potential/skill-gap indicators
- employee insight detail panel
- risk-signal and recommendation lists
- data-confidence information
- distribution panels
- CSV export
- one-click re-analysis

## Model note

`NexusHR Explainable Workforce Engine v2.0` combines attendance, leave, performance-review and employee-profile signals. It is designed for transparent project/demo use and HR decision support. Human review is required before any employment action.
