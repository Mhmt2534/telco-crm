package com.telcocrm.common.persistence.outbox;

/** Lifecycle of an outbox record. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
