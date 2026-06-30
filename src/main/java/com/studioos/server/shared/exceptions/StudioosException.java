package com.studioos.server.shared.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class StudioosException extends RuntimeException {

    private final HttpStatus status;

    public StudioosException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // ─── Convenience factories ───
    public static StudioosException notFound(String message) {
        return new StudioosException(message, HttpStatus.NOT_FOUND);
    }

    public static StudioosException badRequest(String message) {
        return new StudioosException(message, HttpStatus.BAD_REQUEST);
    }

    public static StudioosException unauthorized(String message) {
        return new StudioosException(message, HttpStatus.UNAUTHORIZED);
    }

    public static StudioosException forbidden(String message) {
        return new StudioosException(message, HttpStatus.FORBIDDEN);
    }

    public static StudioosException conflict(String message) {
        return new StudioosException(message, HttpStatus.CONFLICT);
    }
}