package com.tooba.EduEvent.exception;

import com.tooba.EduEvent.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// FIX 4: Added 3 new @ExceptionHandler methods:
//   1. MethodArgumentNotValidException  — handles @Valid failures (field validation errors)
//   2. DataIntegrityViolationException  — handles SQL Server constraint violations (duplicate email etc.)
//   3. Exception (catch-all)            — handles any unexpected error with a clean JSON response
//      Without this, unexpected errors return Spring's default HTML error page instead of JSON.

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Handler 1: ResponseStatusException ──────────────────────────────────
    // Handles all manually thrown exceptions in services
    // e.g. throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatusException(ResponseStatusException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().toString())
                .message(ex.getReason())
                .build();
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // ─── Handler 2: MethodArgumentNotValidException ───────────────────────────
    // FIX 4: Handles @Valid validation failures on request DTOs
    // e.g. if RegisterRequest.email is blank, this returns a clean 400 with all field errors
    // Without this, @Valid failures return a Spring HTML error page
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        // Collect all field error messages into one string
        // e.g. "email: Invalid email format; password: must be at least 6 characters"
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(message)
                .build();

        log.warn("Validation failed: {}", message);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ─── Handler 3: DataIntegrityViolationException ───────────────────────────
    // FIX 4: Handles SQL Server constraint violations
    // e.g. inserting a duplicate email hits the UNIQUE constraint → SQL Server throws this
    // Without this, the user gets a raw SQL error message or HTML page
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityException(DataIntegrityViolationException ex) {
        // Give a clean message — never expose raw SQL error details to the client
        String message = "A record with this data already exists. "
                + "Please check for duplicate email or conflicting values.";

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Conflict")
                .message(message)
                .build();

        // Log the real cause for debugging — but don't send it to the client
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ─── Handler 4: Catch-All Exception ──────────────────────────────────────
    // FIX 4: Catches ANY exception not handled above
    // e.g. NullPointerException, IllegalArgumentException, any unexpected runtime error
    // Without this, the user gets Spring's default HTML error page (Whitelabel Error Page)
    // This ensures the API always returns clean JSON regardless of what goes wrong
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Something went wrong. Please try again later.")
                .build();

        // Log the full stack trace for debugging
        log.error("Unhandled exception: ", ex);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}