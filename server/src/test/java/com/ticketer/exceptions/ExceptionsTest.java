package com.ticketer.exceptions;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExceptionsTest {

    @Test
    public void testTicketerException() {
        TicketerException e = new TicketerException("Error", 400);
        assertEquals("Error", e.getMessage());
        assertEquals(400, e.getStatusCode());
    }

    @Test
    public void testEntityNotFoundException() {
        EntityNotFoundException e = new EntityNotFoundException("Entity not found");
        assertEquals("Entity not found", e.getMessage());
        assertEquals(404, e.getStatusCode());
    }

    @Test
    public void testInvalidInputException() {
        InvalidInputException e = new InvalidInputException("Invalid input");
        assertEquals("Invalid input", e.getMessage());
        assertEquals(400, e.getStatusCode());
    }

    @Test
    public void testInvalidStateException() {
        InvalidStateException e = new InvalidStateException("Invalid state");
        assertEquals("Invalid state", e.getMessage());
        assertEquals(409, e.getStatusCode());
    }

    @Test
    public void testResourceConflictException() {
        ResourceConflictException e = new ResourceConflictException("Conflict");
        assertEquals("Conflict", e.getMessage());
        assertEquals(409, e.getStatusCode());
    }

    @Test
    public void testStorageException() {
        StorageException e = new StorageException("Storage error");
        assertEquals("Storage error", e.getMessage());
        assertEquals(500, e.getStatusCode());

        Exception cause = new RuntimeException("Cause");
        StorageException e2 = new StorageException("Storage error", cause);
        assertEquals("Storage error", e2.getMessage());
        assertEquals(cause, e2.getCause());
        assertEquals(500, e2.getStatusCode());
    }

    @Test
    public void testValidationException() {
        ValidationException e = new ValidationException("Validation failed");
        assertEquals("Validation failed", e.getMessage());
        assertEquals(400, e.getStatusCode());
    }
}
