CREATE TABLE IF NOT EXISTS interview_schedules (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    interview_date DATE NOT NULL,
    interview_time TIME NOT NULL,
    location VARCHAR(300) NOT NULL,
    message TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    sent_by VARCHAR(120) NOT NULL,
    sent_by_role VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT interview_schedules_status_check CHECK (status IN ('SCHEDULED','RESCHEDULED','COMPLETED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_interview_schedules_application
    ON interview_schedules(application_id);
