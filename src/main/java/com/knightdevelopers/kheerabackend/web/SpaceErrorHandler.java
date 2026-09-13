package com.knightdevelopers.kheerabackend.web;

import com.knightdevelopers.kheerabackend.controller.SpaceLifecycleController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SpaceLifecycleController.class)
public class SpaceErrorHandler {
    public record ErrorBody(String code, String message, Map<String, String> fieldErrors) { }
    @ExceptionHandler(SpaceApiException.class)
    public ResponseEntity<ErrorBody> handle(SpaceApiException ex) {
        return ResponseEntity.status(ex.status()).body(new ErrorBody(ex.code(), ex.getMessage(), ex.fieldErrors()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> malformed(Exception ex) {
        // Jackson wraps field validation exceptions; only expose our safe messages.
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SpaceApiException validation) return handle(validation);
            cause = cause.getCause();
        }
        return handle(SpaceApiException.invalid("request", "Invalid JSON body or resource identifier."));
    }
}
