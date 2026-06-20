package com.markovitz.portfolioservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now(), "status", 400, "errors", fieldErrors));
    }

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PortfolioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now(), "status", 404, "error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now(), "status", 400, "error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of(
                "timestamp", LocalDateTime.now(), "status", 500,
                "error", "Erro interno: " + ex.getMessage()));
    }
}
