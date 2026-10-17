ALTER TABLE payroll
    ADD COLUMN IF NOT EXISTS gross_salary NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS bonus NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overtime_pay NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS provident_fund NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS professional_tax NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS other_deductions NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_date DATE,
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

UPDATE payroll
SET gross_salary = COALESCE(basic_salary,0) + COALESCE(allowances,0) + COALESCE(bonus,0) + COALESCE(overtime_pay,0)
WHERE gross_salary IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payroll_employee_month
    ON payroll(employee_id, payroll_month);
