package com.ticketer.api;

import java.time.Instant;

public record ApiResponse<T>(
        ApiStatus status,
        T payload,
        String message,
        String timestamp) {
    public ApiResponse(ApiStatus status, T payload, String message) {
        this(status, payload, message, Instant.now().toString());
    }

    public static <T> ApiResponse<T> success(T payload) {
        return new ApiResponse<>(ApiStatus.SUCCESS, payload, null);
    }

    public static <T> ApiResponse<T> success(T payload, String message) {
        return new ApiResponse<>(ApiStatus.SUCCESS, payload, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ApiStatus.ERROR, null, message);
    }
}
