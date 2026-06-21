package com.telcox.common.core.exception;

/** Thrown when a domain invariant / business rule is violated. Maps to HTTP 422. */
public class BusinessRuleException extends BaseBusinessException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, cause);
    }
}
