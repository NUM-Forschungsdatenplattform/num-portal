package org.highmed.service.exception;

import lombok.Getter;

@Getter
public class IllegalArgumentException extends RuntimeException {

    private final Class<?> entity;

    private final String paramValue;

    private final String message;

    public IllegalArgumentException(Class<?> entity, String message) {
        super(message);
        this.entity = entity;
        this.paramValue = message;
        this.message = message;
    }

}