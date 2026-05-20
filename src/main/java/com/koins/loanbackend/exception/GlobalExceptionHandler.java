package com.koins.loanbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return new ApiErrorResponse(400, "Bad Request", message, req.getRequestURI());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return new ApiErrorResponse(400, "Bad Request",
            "Required request header '" + ex.getHeaderName() + "' is missing", req.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return new ApiErrorResponse(409, "Conflict", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return new ApiErrorResponse(404, "Not Found", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return new ApiErrorResponse(422, "Unprocessable Entity", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex, HttpServletRequest req) {
        return new ApiErrorResponse(500, "Internal Server Error", "An unexpected error occurred", req.getRequestURI());
    }
}
