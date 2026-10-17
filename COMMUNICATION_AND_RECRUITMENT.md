# NexusHR Notification, Email, SMS and Recruitment Communication

## Implemented communication model

NexusHR now treats email as the primary channel for detailed information, in-app notifications as the auditable employee inbox, and SMS as a short alert channel.

### Sender identity
Every new in-app communication stores and displays:
- sender username
- sender role (ADMIN / HR / MANAGER / SYSTEM)
- subject
- message
- communication type
- created date/time
- read/unread state

Existing notification records are migrated to sender `SYSTEM` by Flyway V11.

### Communication Center
ADMIN / HR / MANAGER can send messages to:
- one employee
- a department
- all active employees

The form supports:
- Subject
- Message
- Type
- quick message templates
- Email delivery (enabled by default)
- short SMS alert

### Email
Email carries the full message. Gmail SMTP is supported with environment variables only; credentials must not be committed.

Required `.env` values for Gmail:

```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_sender_email
MAIL_PASSWORD=your_google_app_password
MAIL_FROM=your_sender_email
```

Use a Google App Password rather than the normal Gmail password.

### SMS
SMS is intentionally short. It can run in two modes:

```env
SMS_PROVIDER=SIMULATION
SMS_SIMULATION_ENABLED=true
```

or, after Twilio is configured:

```env
SMS_PROVIDER=TWILIO
SMS_SIMULATION_ENABLED=true
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM_NUMBER=
```

If Twilio is selected but credentials are incomplete, NexusHR falls back to simulation when simulation is enabled.

## Approval email workflow

When an employee submits a leave request, NexusHR emails enabled ADMIN / HR / MANAGER users that have a recovery email configured. The email contains employee name, leave type, dates, number of days, reason, and tells the reviewer where to approve/reject the request.

When the request is approved or rejected:
- the employee receives an in-app notification
- the notification identifies the sender username and role
- the employee receives an email with the full decision and review comment

## Recruitment job alerts

The Recruitment form can optionally notify all active employees when a job is created or updated.

A job alert contains:
- custom Subject
- custom employee message
- in-app alert to every active employee
- full email
- generated NexusHR PDF attachment containing position, department, location, employment type, status, dates, salary range, description and requirements
- optional short SMS alert
- sender username and role

Existing jobs also provide a **Send Alert** action using the standard NexusHR job-alert template.

API endpoint:

```text
POST /api/jobs/{jobId}/announce
```

Example body:

```json
{
  "subject": "Internal Opportunity - Java Developer",
  "message": "A new opportunity is now open. Please review the attached PDF.",
  "sendEmail": true,
  "sendSms": false
}
```

## Username visibility

The signed-in username is now explicitly displayed in the main ADMIN / HR / MANAGER header. Employee Self-Service also displays the employee login username in the hero section. New employee notifications display who sent each message.
