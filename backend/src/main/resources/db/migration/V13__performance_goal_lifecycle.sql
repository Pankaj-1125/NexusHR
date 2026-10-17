ALTER TABLE performance_reviews
    DROP CONSTRAINT IF EXISTS performance_reviews_status_check;

ALTER TABLE performance_reviews
    ADD CONSTRAINT performance_reviews_status_check
    CHECK (status IN ('DRAFT','SELF_REVIEW','SUBMITTED','MANAGER_REVIEW','COMPLETED'));

ALTER TABLE performance_reviews
    ADD COLUMN IF NOT EXISTS review_cycle VARCHAR(100),
    ADD COLUMN IF NOT EXISTS review_type VARCHAR(50) DEFAULT 'ANNUAL',
    ADD COLUMN IF NOT EXISTS competency_score NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS goal_score NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS final_score NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS performance_goals (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    success_metric TEXT,
    start_date DATE NOT NULL,
    target_date DATE NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    weight INTEGER NOT NULL DEFAULT 100,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    manager_comment TEXT,
    employee_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_performance_goal_employee FOREIGN KEY(employee_id)
        REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_performance_goal_progress CHECK(progress BETWEEN 0 AND 100),
    CONSTRAINT chk_performance_goal_weight CHECK(weight BETWEEN 1 AND 100),
    CONSTRAINT chk_performance_goal_status CHECK(status IN ('DRAFT','ACTIVE','COMPLETED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_performance_goals_employee ON performance_goals(employee_id);
CREATE INDEX IF NOT EXISTS idx_performance_goals_status ON performance_goals(status);
