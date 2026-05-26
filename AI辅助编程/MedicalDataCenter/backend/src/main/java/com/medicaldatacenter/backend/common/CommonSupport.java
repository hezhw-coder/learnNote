package com.medicaldatacenter.backend.common;

import org.slf4j.MDC;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;

public final class CommonSupport {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = CommonSupport.class.getName() + ".requestId";
    public static final String REQUEST_STARTED_AT_ATTRIBUTE = CommonSupport.class.getName() + ".requestStartedAt";

    private CommonSupport() {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, currentRequestId(), OffsetDateTime.now());
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(0, message, null, currentRequestId(), OffsetDateTime.now());
    }

    public static String currentRequestId() {
        String requestId = RequestIdContext.current();
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            Object value = requestAttributes.getAttribute(REQUEST_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof String requestIdFromRequest && StringUtils.hasText(requestIdFromRequest)) {
                return requestIdFromRequest;
            }
        }
        return UUID.randomUUID().toString();
    }

    public static String currentRemoteIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = normalizeHeader(request.getHeader("X-Forwarded-For"));
        if (StringUtils.hasText(forwardedFor)) {
            int separator = forwardedFor.indexOf(',');
            return separator > 0 ? forwardedFor.substring(0, separator).trim() : forwardedFor;
        }
        String realIp = normalizeHeader(request.getHeader("X-Real-IP"));
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }

    public static String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String userAgent = normalizeHeader(request.getHeader("User-Agent"));
        return StringUtils.hasText(userAgent) ? userAgent : "unknown";
    }

    public static Long currentDurationMs() {
        Long duration = RequestStartContext.durationMs();
        if (duration != null) {
            return duration;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            Object value = requestAttributes.getAttribute(REQUEST_STARTED_AT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof Long startedAt) {
                return Math.max(0, System.currentTimeMillis() - startedAt);
            }
        }
        return null;
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

    public static final class RequestIdContext {
        private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

        private RequestIdContext() {
        }

        public static void setCurrent(String requestId) {
            if (!StringUtils.hasText(requestId)) {
                clear();
                return;
            }
            REQUEST_ID_HOLDER.set(requestId);
            MDC.put("requestId", requestId);
        }

        public static String current() {
            return REQUEST_ID_HOLDER.get();
        }

        public static void clear() {
            REQUEST_ID_HOLDER.remove();
            MDC.remove("requestId");
        }
    }

    public static final class RequestStartContext {
        private static final ThreadLocal<Long> REQUEST_START_HOLDER = new ThreadLocal<>();

        private RequestStartContext() {
        }

        public static void setCurrent(long startedAt) {
            REQUEST_START_HOLDER.set(startedAt);
        }

        public static Long durationMs() {
            Long startedAt = REQUEST_START_HOLDER.get();
            if (startedAt == null) {
                return null;
            }
            return Math.max(0, System.currentTimeMillis() - startedAt);
        }

        public static void clear() {
            REQUEST_START_HOLDER.remove();
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private static String normalizeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace("\r", "").replace("\n", "").trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
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
                            currentRequestId(),
                            OffsetDateTime.now()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
            String message = exception.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .orElse("validation failed");
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, message, null, currentRequestId(), OffsetDateTime.now()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, exception.getMessage(), null, currentRequestId(),
                            OffsetDateTime.now()));
        }
    }
}
