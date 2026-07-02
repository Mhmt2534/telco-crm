-- ─────────────────────────────────────────────────────────────
-- V1: Create otp_attempt table
-- Tracks OTP verification attempts per phone number.
-- Supports rate-limiting: 3 failed attempts → 5-minute lock.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE otp_attempt
(
    id            BIGSERIAL                NOT NULL,
    phone         VARCHAR(15)              NOT NULL,
    attempt_count INT                      NOT NULL DEFAULT 0,
    locked_until  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_otp_attempt PRIMARY KEY (id),
    CONSTRAINT uq_otp_attempt_phone UNIQUE (phone)
);

COMMENT ON TABLE otp_attempt IS
    'Stores per-phone OTP attempt counters and temporary lock state for customer login flow.';
COMMENT ON COLUMN otp_attempt.phone IS
    'E.164 format phone number, e.g. 905XXXXXXXXX';
COMMENT ON COLUMN otp_attempt.attempt_count IS
    'Consecutive failed OTP verification attempts since last reset.';
COMMENT ON COLUMN otp_attempt.locked_until IS
    'When set, the phone is blocked from verifying OTP until this timestamp.';
