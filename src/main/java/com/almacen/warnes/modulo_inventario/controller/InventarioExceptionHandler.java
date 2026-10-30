package com.almacen.warnes.modulo_inventario.controller;

import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.almacen.warnes.modulo_inventario")
public class InventarioExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex) {
        String mensaje = extraerMensajeLimpio(ex);
        HttpStatus status = determinarHttpStatus(mensaje, ex);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, String>> handleSQLException(SQLException ex) {
        String mensaje = extraerMensajeLimpio(ex);
        HttpStatus status = determinarHttpStatus(mensaje, ex);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", ex.getMessage());

        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("no encontrado")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String mensaje = (fieldError != null)
                ? fieldError.getDefaultMessage()
                : "Error de validacion en los datos enviados";

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mensaje", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private HttpStatus determinarHttpStatus(String mensaje, Throwable ex) {
        String texto = (mensaje != null ? mensaje : "").toLowerCase();
        if (texto.contains("duplicad") || texto.contains("ya existe") || texto.contains("ya registrado")
                || texto.contains("unique") || texto.contains("llave duplicada")
                || ex instanceof DataIntegrityViolationException) {
            return HttpStatus.CONFLICT; // 409
        }
        return HttpStatus.BAD_REQUEST; // 400
    }

    private String extraerMensajeLimpio(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root != root.getCause()) {
            root = root.getCause();
        }

        String raw = root.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            raw = ex.getMessage();
        }
        if (raw == null) {
            return "Error al ejecutar la operacion en la base de datos";
        }

        String cleaned = raw.trim();
        if (cleaned.startsWith("ERROR: ")) {
            cleaned = cleaned.substring(7);
        }

        int whereIdx = cleaned.indexOf("\n  Where:");
        if (whereIdx != -1) {
            cleaned = cleaned.substring(0, whereIdx);
        }
        int dondeIdx = cleaned.indexOf("\n  Donde:");
        if (dondeIdx != -1) {
            cleaned = cleaned.substring(0, dondeIdx);
        }

        return cleaned.trim();
    }
}

