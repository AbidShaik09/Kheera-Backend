package com.knightdevelopers.kheerabackend.web;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class SpaceApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fieldErrors;
    private SpaceApiException(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        super(message); this.status = status; this.code = code; this.fieldErrors = fieldErrors;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Map<String, String> fieldErrors() { return fieldErrors; }
    public static SpaceApiException invalid(String field, String message) {
        return new SpaceApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request.", Map.of(field, message));
    }
    public static SpaceApiException unauthorized() {
        return new SpaceApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "An active account is required.", Map.of());
    }
    public static SpaceApiException notFound() {
        return new SpaceApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Space not found.", Map.of());
    }
    public static SpaceApiException forbidden() {
        return new SpaceApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission for this action.", Map.of());
    }
    public static SpaceApiException resourceNotFound() {
        return new SpaceApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found.", Map.of());
    }
    public static SpaceApiException conflict(String code, String message) {
        return new SpaceApiException(HttpStatus.CONFLICT, code, message, Map.of());
    }
}
