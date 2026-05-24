package com.medicaldatacenter.backend.common;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public final class CommonSupport {

    private CommonSupport() {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, UUID.randomUUID().toString(), OffsetDateTime.now());
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(0, message, null, UUID.randomUUID().toString(), OffsetDateTime.now());
    }

    public record ApiResponse<T>(
            int code,
            String message,
            T data,
            String requestId,
            OffsetDateTime timestamp) {
    }

    public static class BusinessException extends RuntimeException {
        private final HttpStatus status;

        public BusinessException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
            return ResponseEntity.status(exception.getStatus())
                    .body(new ApiResponse<>(
                            exception.getStatus().value(),
                            exception.getMessage(),
                            null,
                            UUID.randomUUID().toString(),
                            OffsetDateTime.now()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
            String message = exception.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .orElse("validation failed");
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, message, null, UUID.randomUUID().toString(), OffsetDateTime.now()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, exception.getMessage(), null, UUID.randomUUID().toString(),
                            OffsetDateTime.now()));
        }
    }
}
