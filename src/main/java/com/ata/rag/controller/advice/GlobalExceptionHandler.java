package com.ata.rag.controller.advice;

import com.ata.rag.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return buildResponse(status, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleNotImplemented(
            UnsupportedOperationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_IMPLEMENTED, exception.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
