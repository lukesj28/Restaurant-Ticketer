package com.ticketer.exceptions;

public class PrinterException extends TicketerException {

    public PrinterException(String message) {
        super(message, 503);
    }

    public PrinterException(String message, Throwable cause) {
        super(message, cause, 503);
    }
}
