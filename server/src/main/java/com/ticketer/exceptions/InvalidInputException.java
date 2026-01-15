package com.ticketer.exceptions;

public class InvalidInputException extends TicketerException {
    public InvalidInputException(String message) {
        super(message, 400);
    }
}
