package com.riskregister.common;

/** A malformed request parameter that Bean Validation cannot express (mapped to 400). */
public class BadRequestException extends RuntimeException {
    private final String field;

    public BadRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
