package com.ticketer.exceptions;

public class ActionNotAllowedException extends TicketerException {
    public ActionNotAllowedException(String message) {
        super(message, 409);
    }
}
