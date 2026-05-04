package com.aulix.aulix_backend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AulixException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AulixException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // commons
    public static AulixException notFound(String message) {
        return new AulixException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static AulixException badRequest(String message) {
        return  new AulixException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static AulixException conflict(String message) {
        return new AulixException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static AulixException forbidden(String message) {
        return new AulixException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static AulixException unauthorized(String message) {
        return new AulixException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}

