package com.ticketer.exceptions;

public class TicketerException extends RuntimeException {
    public TicketerException(String message) {
        super(message);
    }

    public TicketerException(String message, Throwable cause) {
        super(message, cause);
    }
}
