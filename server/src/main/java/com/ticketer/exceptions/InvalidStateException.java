package com.ticketer.exceptions;

public class InvalidStateException extends TicketerException {
    public InvalidStateException(String message) {
        super(message, 409);
    }
}
