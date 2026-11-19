package com.study.task.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> api(ApiException e) {
        return ResponseEntity.status(e.status)
                .body(Map.of("error", e.getMessage(), "status", e.status.value()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badLogin() {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials", "status", 401));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                "error",
                                "Validation failed",
                                "errors",
                                e.getBindingResult().getFieldErrors().stream()
                                        .map(
                                                f ->
                                                        Map.of(
                                                                "field",
                                                                f.getField(),
                                                                "message",
                                                                Objects.requireNonNullElse(f.getDefaultMessage(), "Invalid")))
                                        .toList()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("error", "Conflict", "status", 409));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<?> malformed() {
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed request", "status", 400));
    }
}
