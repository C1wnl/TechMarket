package com.marketplace.auth.exception;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarResponseStatusException(
            ResponseStatusException ex) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("status", ex.getStatusCode().value());
        respuesta.put("message", ex.getReason());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarErroresDeValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errores.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity
                .badRequest()
                .body(errores);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> manejarJwtException(
            JwtException ex) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("status", 401);
        respuesta.put("message", "El token no es válido");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(respuesta);
    }
}