package org.highmed.numportal.service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UsernameNotFoundOrNoLongerActiveException extends RuntimeException  {

    private final Class<?> entity;
    private final String parameter;
}
