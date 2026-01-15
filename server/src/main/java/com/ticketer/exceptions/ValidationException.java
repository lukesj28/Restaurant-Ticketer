package com.ticketer.exceptions;

public class ValidationException extends TicketerException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
