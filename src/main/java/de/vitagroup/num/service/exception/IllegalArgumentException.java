package de.vitagroup.num.service.exception;

import lombok.Getter;

@Getter
public class IllegalArgumentException extends RuntimeException {

    private final Class<?> entity;

    private final String paramValue;

    private final String message;

/*    public IllegalArgumentException(Class<?> entity, String paramValue, String parameter) {
        super(parameter);
        this.entity = entity;
        this.paramValue = paramValue;
        this.parameter = parameter;
    }*/

    public IllegalArgumentException(Class<?> entity, String message) {
        super(message);
        this.entity = entity;
        this.paramValue = message;
        this.message = message;
    }

}