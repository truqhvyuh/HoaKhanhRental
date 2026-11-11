package com.hoakhanh.rental;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        for (FieldError fieldError :
                ex.getBindingResult().getFieldErrors()) {

            fields.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() == null
                            ? "Invalid value"
                            : fieldError.getDefaultMessage()
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Validation failed");
        body.put("fields", fields);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());

        String error = httpStatus == null
                ? "HTTP Error"
                : httpStatus.getReasonPhrase();

        String message = ex.getReason() == null
                ? error
                : ex.getReason();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", statusCode.value());
        body.put("error", error);
        body.put("message", message);

        return ResponseEntity
                .status(statusCode)
                .body(body);
    }
}
