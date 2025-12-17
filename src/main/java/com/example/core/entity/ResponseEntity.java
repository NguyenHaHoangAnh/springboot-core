package com.example.core.entity;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

public class ResponseEntity<T> extends HttpEntity<T> {
    private final Object httpStatus;

    private ResponseEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers, Object httpStatus) {
        super(body, headers);
        Assert.notNull(httpStatus, "HttpStatus must not be null");
        this.httpStatus = httpStatus;
    }

    public ResponseEntity(HttpStatus httpStatus) {
        this(null, null, httpStatus);
    }

    public ResponseEntity(T body, HttpStatus httpStatus) {
        this(body, null, httpStatus);
    }
}
