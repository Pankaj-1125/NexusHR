CREATE TABLE performance_feedback (
                                      id BIGSERIAL PRIMARY KEY,

                                      review_id BIGINT NOT NULL,
                                      employee_id BIGINT NOT NULL,
                                      reviewer_id BIGINT,

                                      feedback_type VARCHAR(30) NOT NULL,
                                      rating INTEGER NOT NULL,

                                      comments TEXT,
                                      strengths TEXT,
                                      areas_for_improvement TEXT,

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_performance_feedback_review
                                          FOREIGN KEY (review_id)
                                              REFERENCES performance_reviews(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_performance_feedback_employee
                                          FOREIGN KEY (employee_id)
                                              REFERENCES employees(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_performance_feedback_reviewer
                                          FOREIGN KEY (reviewer_id)
                                              REFERENCES employees(id)
                                              ON DELETE SET NULL,

                                      CONSTRAINT chk_performance_feedback_rating
                                          CHECK (rating >= 1 AND rating <= 5),

                                      CONSTRAINT chk_performance_feedback_type
                                          CHECK (
                                              feedback_type IN (
                                                                'SELF',
                                                                'MANAGER',
                                                                'PEER',
                                                                'SUBORDINATE'
                                                  )
                                              )
);

CREATE INDEX idx_performance_feedback_review
    ON performance_feedback(review_id);

CREATE INDEX idx_performance_feedback_employee
    ON performance_feedback(employee_id);

CREATE INDEX idx_performance_feedback_reviewer
    ON performance_feedback(reviewer_id);