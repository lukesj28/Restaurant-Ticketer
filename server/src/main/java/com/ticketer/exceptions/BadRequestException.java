package com.ticketer.exceptions;

public class BadRequestException extends TicketerException {
    public BadRequestException(String message) {
        super(message);
    }
}
