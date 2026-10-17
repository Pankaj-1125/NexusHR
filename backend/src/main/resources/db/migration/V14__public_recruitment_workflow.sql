-- NexusHR public recruitment portal + two-stage shortlist workflow
ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS qualification VARCHAR(180),
    ADD COLUMN IF NOT EXISTS experience_years NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS skills TEXT,
    ADD COLUMN IF NOT EXISTS current_city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS expected_salary NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS notice_period VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source VARCHAR(80),
    ADD COLUMN IF NOT EXISTS application_reference VARCHAR(60),
    ADD COLUMN IF NOT EXISTS manager_shortlisted_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS manager_shortlisted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS admin_approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS admin_approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS recruiter_notes TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_job_applications_reference
    ON job_applications(application_reference)
    WHERE application_reference IS NOT NULL;

ALTER TABLE job_applications
    DROP CONSTRAINT IF EXISTS job_applications_status_check;

ALTER TABLE job_applications
    ADD CONSTRAINT job_applications_status_check CHECK (
        status IN (
            'APPLIED',
            'SCREENING',
            'MANAGER_SHORTLISTED',
            'ADMIN_APPROVED',
            'INTERVIEW',
            'OFFER',
            'HIRED',
            'REJECTED'
        )
    );
