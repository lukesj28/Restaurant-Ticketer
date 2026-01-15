package com.ticketer.exceptions;

public class ResourceConflictException extends TicketerException {
    public ResourceConflictException(String message) {
        super(message, 409);
    }
}
