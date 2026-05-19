package com.koins.loanbackend.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends RuntimeException{
    private String message;
    private String httpStatus;

    public UnauthorizedException(String message, HttpStatus httpStatus) {
        this.httpStatus = String.valueOf(httpStatus);
        this.message = message;

    }


    public String getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(String httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
