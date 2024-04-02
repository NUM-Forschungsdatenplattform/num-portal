package org.highmed.numportal.service.exception.implementation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BadCredentialsException extends RuntimeException  {

    private final Class<?> entity;

    private final String parameter;
}
