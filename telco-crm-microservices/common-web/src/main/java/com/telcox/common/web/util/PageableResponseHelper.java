package com.telcox.common.web.util;

import org.springframework.data.domain.Page;

import com.telcox.common.core.model.PageResponse;

/** Converts a Spring Data {@link Page} into the stable {@link PageResponse} API envelope. */
public final class PageableResponseHelper {

    private PageableResponseHelper() {
    }

    public static <T> PageResponse<T> toResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
