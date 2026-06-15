package com.telcocrm.common.core.exception;

public abstract class BaseBusinessException extends RuntimeException {

    protected BaseBusinessException(String message) {
        super(message);
    }

    protected BaseBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
