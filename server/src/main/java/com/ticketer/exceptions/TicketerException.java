package com.ticketer.exceptions;

public class TicketerException extends RuntimeException {
    private final int statusCode;

    public TicketerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public TicketerException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
