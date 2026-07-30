package com.kartify.api.shared.dto;

public record ApiResponse<T> (
    boolean success,
    String message,
    T payload
) {

    public static <T> ApiResponse<T> success(String message, T payload) {
        return new ApiResponse<>(true, message, payload);
    }

    public static <T> ApiResponse<T> success(T payload) {
        return new ApiResponse<>(true, null, payload);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
