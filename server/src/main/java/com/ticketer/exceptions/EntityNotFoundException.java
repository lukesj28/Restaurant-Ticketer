package com.ticketer.exceptions;

public class EntityNotFoundException extends TicketerException {
    public EntityNotFoundException(String message) {
        super(message, 404);
    }
}
