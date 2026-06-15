package com.telcocrm.common.core.exception;

/**
 * Root of the platform business-exception hierarchy. Every business exception carries an
 * {@link ErrorCode} which the global exception handler maps to an RFC 7807 response.
 */
public abstract class BaseBusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected BaseBusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseBusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
