package com.telcox.common.core.model;

import java.util.UUID;

/** Minimal cross-service identity mapping; contains no customer PII. */
public record CustomerIdentityResponse(UUID customerId) {
}
