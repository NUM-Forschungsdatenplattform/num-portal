package de.vitagroup.num.service.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final Class<?> entity;

    private final String paramValue;

    private final String message;

    public BadRequestException(Class<?> entity, String paramValue, String message) {
        super(message);
        this.entity = entity;
        this.paramValue = paramValue;
        this.message = message;
    }

    public BadRequestException(Class<?> entity, String message) {
        super(message);
        this.entity = entity;
        this.paramValue = message;
        this.message = message;
    }

}
