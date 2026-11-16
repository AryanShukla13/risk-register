package com.riskregister.common;

/** A well-formed request that conflicts with a business rule (mapped to 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
