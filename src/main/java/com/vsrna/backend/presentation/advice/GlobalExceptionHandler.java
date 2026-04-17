package com.vsrna.backend.presentation.advice;

import com.vsrna.backend.domain.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        log.error("ApiException guid:{} code:{} message:{}", ex.getGuid(), ex.getCode(), ex.getMessage());

        Map<String, Object> body = Map.of(
                "guid", ex.getGuid(),
                "code", ex.getCode(),
                "message", ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", body));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> "Поле '" + err.getField() + "': " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Ошибка валидации данных запроса";
        }

        ApiException apiEx = ApiException.validationError(message);
        log.error("Validation error guid:{} message:{}", apiEx.getGuid(), apiEx.getMessage());

        Map<String, Object> body = Map.of(
                "guid", apiEx.getGuid(),
                "code", apiEx.getCode(),
                "message", apiEx.getMessage()
        );
        return ResponseEntity.status(apiEx.getStatus()).body(Map.of("error", body));
    }
}
