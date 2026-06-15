package com.telcocrm.common.core.model;

import java.util.List;

/**
 * Stable, serialization-friendly pagination envelope. Use this for list endpoints instead of
 * returning Spring's {@code PageImpl} directly (whose JSON shape is unstable across versions).
 * Build it via {@code PageableResponseHelper} in {@code common-web}.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
