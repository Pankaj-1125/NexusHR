ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS final_selection_message TEXT,
    ADD COLUMN IF NOT EXISTS required_documents TEXT,
    ADD COLUMN IF NOT EXISTS reporting_instructions TEXT,
    ADD COLUMN IF NOT EXISTS final_approved_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS final_approved_at TIMESTAMP;

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
            'INTERVIEW_COMPLETED',
            'OFFER',
            'HIRED',
            'REJECTED'
        )
    );
