package com.telcox.common.core.exception;

/** Thrown when creating a resource that conflicts with an existing one. Maps to HTTP 409. */
public class DuplicateResourceException extends BaseBusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
