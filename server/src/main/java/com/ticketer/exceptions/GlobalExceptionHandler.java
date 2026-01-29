package com.ticketer.exceptions;

import com.ticketer.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketerException.class)
    public ResponseEntity<ApiResponse<Object>> handleTicketerException(TicketerException e) {
        logger.warn("TicketerException", e);
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundException(EntityNotFoundException e) {
        logger.warn("EntityNotFoundException", e);
        return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        logger.error("Unhandled exception occurred", e);
        return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
    }
}
