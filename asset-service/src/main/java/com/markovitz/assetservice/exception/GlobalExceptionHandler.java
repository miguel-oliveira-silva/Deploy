package com.markovitz.assetservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Tratamento centralizado de exceções — mesmo padrão do user-service */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssetNotFound(AssetNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 404);
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(TickerAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleTickerExists(TickerAlreadyExistsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 409);
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Erro interno no servidor");
        System.err.println("[ERROR] " + ex.getMessage());
        return ResponseEntity.internalServerError().body(response);
    }
}
