package com.ticketer.exceptions;

import com.ticketer.api.ApiResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketerException.class)
    @ResponseBody
    public ApiResponse<Object> handleTicketerException(TicketerException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    public ApiResponse<Object> handleEntityNotFoundException(EntityNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResponse<Object> handleGenericException(Exception e) {
        return ApiResponse.error(e.getMessage());
    }
}
