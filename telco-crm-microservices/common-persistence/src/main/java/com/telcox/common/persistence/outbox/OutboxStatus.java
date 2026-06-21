package com.telcox.common.persistence.outbox;

/** Lifecycle of an outbox record. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
