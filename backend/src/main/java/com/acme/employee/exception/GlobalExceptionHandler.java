package com.acme.employee.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import jakarta.validation.ConstraintViolationException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ErrorResponse> handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ErrorResponse> handleBind(WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, Object> details = ex.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", exchange, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ErrorResponse> handleConstraint(ConstraintViolationException ex, ServerWebExchange exchange) {
        Map<String, Object> details = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existing, replacement) -> existing
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint violation", exchange, details);
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ErrorResponse> handleGeneric(Throwable ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), exchange, Map.of());
    }

    // Hardening: return 409 Conflict for duplicate key errors (e.g., unique indexes)
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Mono<ErrorResponse> handleDuplicateKey(org.springframework.dao.DuplicateKeyException ex,
                                                  ServerWebExchange exchange) {
        return buildResponse(HttpStatus.CONFLICT, getRootMessage(ex), exchange, Map.of());
    }

    // Fallback if driver exception bubbles up without translation
    @ExceptionHandler(com.mongodb.MongoWriteException.class)
    public Mono<ErrorResponse> handleMongoWrite(com.mongodb.MongoWriteException ex,
                                                ServerWebExchange exchange) {
        return buildResponse(HttpStatus.CONFLICT, ex.getError() != null ? ex.getError().getMessage() : ex.getMessage(), exchange, Map.of());
    }

    private String getRootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : ex.getMessage();
    }

    private Mono<ErrorResponse> buildResponse(HttpStatus status,
                                              String message,
                                              ServerWebExchange exchange,
                                              Map<String, Object> details) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                details.isEmpty() ? null : details
        );
        return Mono.just(body);
    }
}
