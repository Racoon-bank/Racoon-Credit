-- Full schema migration for credit-service based on current JPA entities.
-- PostgreSQL only.
-- Can be used both for clean setup and for upgrading an older schema.

BEGIN;

-- ============================================================================
-- Base tables from current code
-- ============================================================================

CREATE TABLE IF NOT EXISTS credit_tariffs (
    id bigserial PRIMARY KEY,
    name varchar(255) NOT NULL UNIQUE,
    interest_rate numeric(5, 2) NOT NULL,
    due_date date NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credits (
    id bigserial PRIMARY KEY,
    owner_id varchar(255) NOT NULL,
    bank_account_id varchar(255),
    currency varchar(10) NOT NULL DEFAULT 'RUB',
    tariff_id bigint NOT NULL,
    amount numeric(15, 2) NOT NULL,
    remaining_amount numeric(15, 2) NOT NULL,
    monthly_payment numeric(15, 2) NOT NULL,
    duration_months integer NOT NULL,
    remaining_months integer NOT NULL,
    accumulated_penalty numeric(15, 2) NOT NULL DEFAULT 0.00,
    overdue_days integer NOT NULL DEFAULT 0,
    status varchar(50) NOT NULL,
    issue_date timestamp NOT NULL,
    next_payment_date timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credit_applications (
    id bigserial PRIMARY KEY,
    owner_id varchar(255) NOT NULL,
    bank_account_id varchar(255) NOT NULL,
    currency varchar(10) NOT NULL DEFAULT 'RUB',
    tariff_id bigint NOT NULL,
    amount numeric(15, 2) NOT NULL,
    duration_months integer NOT NULL,
    credit_rating integer NOT NULL,
    status varchar(50) NOT NULL,
    employee_comment varchar(255),
    reviewed_by varchar(255),
    reviewed_at timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_schedule (
    id bigserial PRIMARY KEY,
    credit_id bigint NOT NULL,
    month_number integer NOT NULL,
    payment_date timestamp NOT NULL,
    total_payment numeric(15, 2) NOT NULL,
    interest_payment numeric(15, 2) NOT NULL,
    principal_payment numeric(15, 2) NOT NULL,
    remaining_balance numeric(15, 2) NOT NULL,
    paid boolean NOT NULL DEFAULT false,
    payment_status varchar(50) NOT NULL DEFAULT 'PLANNED',
    paid_penalty_amount numeric(15, 2) NOT NULL DEFAULT 0.00,
    paid_interest_amount numeric(15, 2) NOT NULL DEFAULT 0.00,
    paid_principal_amount numeric(15, 2) NOT NULL DEFAULT 0.00,
    penalty_amount numeric(15, 2) NOT NULL DEFAULT 0.00,
    overdue_days integer NOT NULL DEFAULT 0,
    paid_at timestamp,
    last_penalty_applied_at timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credit_payments (
    id bigserial PRIMARY KEY,
    credit_id bigint NOT NULL,
    amount numeric(15, 2) NOT NULL,
    payment_type varchar(50) NOT NULL,
    payment_date timestamp NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS master_accounts (
    code varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    currency varchar(10) NOT NULL,
    balance numeric(15, 2) NOT NULL DEFAULT 0.00,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Upgrade existing older schemas to current entity state
-- ============================================================================

ALTER TABLE credit_tariffs
    ADD COLUMN IF NOT EXISTS due_date date,
    ADD COLUMN IF NOT EXISTS is_active boolean,
    ADD COLUMN IF NOT EXISTS created_at timestamp;

UPDATE credit_tariffs
SET is_active = true
WHERE is_active IS NULL;

UPDATE credit_tariffs
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE credit_tariffs
    ALTER COLUMN due_date SET NOT NULL,
    ALTER COLUMN is_active SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE credit_tariffs
    ALTER COLUMN is_active SET DEFAULT true,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE credits
    ADD COLUMN IF NOT EXISTS bank_account_id varchar(255),
    ADD COLUMN IF NOT EXISTS currency varchar(10),
    ADD COLUMN IF NOT EXISTS accumulated_penalty numeric(15, 2),
    ADD COLUMN IF NOT EXISTS overdue_days integer,
    ADD COLUMN IF NOT EXISTS next_payment_date timestamp,
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp;

UPDATE credits
SET currency = 'RUB'
WHERE currency IS NULL;

UPDATE credits
SET accumulated_penalty = 0.00
WHERE accumulated_penalty IS NULL;

UPDATE credits
SET overdue_days = 0
WHERE overdue_days IS NULL;

UPDATE credits
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE credits
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE credits
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN accumulated_penalty SET NOT NULL,
    ALTER COLUMN overdue_days SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE credits
    ALTER COLUMN accumulated_penalty SET DEFAULT 0.00,
    ALTER COLUMN overdue_days SET DEFAULT 0,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE credit_applications
    ADD COLUMN IF NOT EXISTS currency varchar(10),
    ADD COLUMN IF NOT EXISTS employee_comment varchar(255),
    ADD COLUMN IF NOT EXISTS reviewed_by varchar(255),
    ADD COLUMN IF NOT EXISTS reviewed_at timestamp,
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp;

UPDATE credit_applications
SET currency = 'RUB'
WHERE currency IS NULL;

UPDATE credit_applications
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE credit_applications
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE credit_applications
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE credit_applications
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE payment_schedule
    ADD COLUMN IF NOT EXISTS payment_status varchar(50),
    ADD COLUMN IF NOT EXISTS paid_penalty_amount numeric(15, 2),
    ADD COLUMN IF NOT EXISTS paid_interest_amount numeric(15, 2),
    ADD COLUMN IF NOT EXISTS paid_principal_amount numeric(15, 2),
    ADD COLUMN IF NOT EXISTS penalty_amount numeric(15, 2),
    ADD COLUMN IF NOT EXISTS overdue_days integer,
    ADD COLUMN IF NOT EXISTS paid_at timestamp,
    ADD COLUMN IF NOT EXISTS last_penalty_applied_at timestamp,
    ADD COLUMN IF NOT EXISTS created_at timestamp;

UPDATE payment_schedule
SET payment_status = CASE
    WHEN COALESCE(paid, false) = true THEN 'PAID'
    ELSE 'PLANNED'
END
WHERE payment_status IS NULL;

UPDATE payment_schedule
SET paid_penalty_amount = 0.00
WHERE paid_penalty_amount IS NULL;

UPDATE payment_schedule
SET paid_interest_amount = 0.00
WHERE paid_interest_amount IS NULL;

UPDATE payment_schedule
SET paid_principal_amount = 0.00
WHERE paid_principal_amount IS NULL;

UPDATE payment_schedule
SET penalty_amount = 0.00
WHERE penalty_amount IS NULL;

UPDATE payment_schedule
SET overdue_days = 0
WHERE overdue_days IS NULL;

UPDATE payment_schedule
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE payment_schedule
    ALTER COLUMN payment_status SET NOT NULL,
    ALTER COLUMN paid_penalty_amount SET NOT NULL,
    ALTER COLUMN paid_interest_amount SET NOT NULL,
    ALTER COLUMN paid_principal_amount SET NOT NULL,
    ALTER COLUMN penalty_amount SET NOT NULL,
    ALTER COLUMN overdue_days SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE payment_schedule
    ALTER COLUMN payment_status SET DEFAULT 'PLANNED',
    ALTER COLUMN paid_penalty_amount SET DEFAULT 0.00,
    ALTER COLUMN paid_interest_amount SET DEFAULT 0.00,
    ALTER COLUMN paid_principal_amount SET DEFAULT 0.00,
    ALTER COLUMN penalty_amount SET DEFAULT 0.00,
    ALTER COLUMN overdue_days SET DEFAULT 0,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE credit_payments
    ADD COLUMN IF NOT EXISTS created_at timestamp;

UPDATE credit_payments
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE credit_payments
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE credit_payments
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE master_accounts
    ADD COLUMN IF NOT EXISTS currency varchar(10),
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp;

UPDATE master_accounts
SET currency = 'RUB'
WHERE currency IS NULL;

UPDATE master_accounts
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE master_accounts
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE master_accounts
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE master_accounts
    ALTER COLUMN balance SET DEFAULT 0.00,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- ============================================================================
-- Foreign keys
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_credits_tariff'
    ) THEN
        ALTER TABLE credits
            ADD CONSTRAINT fk_credits_tariff
            FOREIGN KEY (tariff_id) REFERENCES credit_tariffs(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_credit_applications_tariff'
    ) THEN
        ALTER TABLE credit_applications
            ADD CONSTRAINT fk_credit_applications_tariff
            FOREIGN KEY (tariff_id) REFERENCES credit_tariffs(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payment_schedule_credit'
    ) THEN
        ALTER TABLE payment_schedule
            ADD CONSTRAINT fk_payment_schedule_credit
            FOREIGN KEY (credit_id) REFERENCES credits(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_credit_payments_credit'
    ) THEN
        ALTER TABLE credit_payments
            ADD CONSTRAINT fk_credit_payments_credit
            FOREIGN KEY (credit_id) REFERENCES credits(id);
    END IF;
END $$;

-- ============================================================================
-- Helpful indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_credits_owner_id ON credits(owner_id);
CREATE INDEX IF NOT EXISTS idx_credits_tariff_id ON credits(tariff_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedule_credit_id ON payment_schedule(credit_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedule_payment_status ON payment_schedule(payment_status);
CREATE INDEX IF NOT EXISTS idx_payment_schedule_payment_date ON payment_schedule(payment_date);
CREATE INDEX IF NOT EXISTS idx_credit_payments_credit_id ON credit_payments(credit_id);
CREATE INDEX IF NOT EXISTS idx_credit_applications_owner_id ON credit_applications(owner_id);
CREATE INDEX IF NOT EXISTS idx_credit_applications_status ON credit_applications(status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_master_accounts_currency ON master_accounts(currency);

-- ============================================================================
-- Seed master accounts for supported currencies
-- ============================================================================

DO $$
DECLARE
    base_balance numeric(15, 2);
BEGIN
    SELECT balance INTO base_balance
    FROM master_accounts
    WHERE currency = 'RUB'
    ORDER BY code
    LIMIT 1;

    IF base_balance IS NULL THEN
        base_balance := 1000000.00;
    END IF;

    INSERT INTO master_accounts (code, name, currency, balance, created_at, updated_at)
    VALUES
        ('BANK_MASTER_ACCOUNT_RUB', 'Bank Master Account RUB', 'RUB', base_balance, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('BANK_MASTER_ACCOUNT_USD', 'Bank Master Account USD', 'USD', base_balance, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('BANK_MASTER_ACCOUNT_EUR', 'Bank Master Account EUR', 'EUR', base_balance, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (code) DO NOTHING;
END $$;

COMMIT;
