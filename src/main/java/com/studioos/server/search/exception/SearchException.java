package com.studioos.server.search.exception;

import com.studioos.server.shared.exceptions.StudioosException;
import org.springframework.http.HttpStatus;

public class SearchException extends StudioosException {
    public SearchException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public SearchException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
