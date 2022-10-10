package de.vitagroup.num.service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BadCredentialsException extends RuntimeException  {

    private final Class<?> entity;

    private final String parameter;
}
