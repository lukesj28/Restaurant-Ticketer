package com.ticketer.exceptions;

public class StorageException extends TicketerException {
    public StorageException(String message) {
        super(message, 500);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause, 500);
    }
}
