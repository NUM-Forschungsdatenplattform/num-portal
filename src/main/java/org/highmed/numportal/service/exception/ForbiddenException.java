package org.highmed.numportal.service.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String message;

  public ForbiddenException(Class<?> entity, String paramValue, String message) {
    super(message);
    this.entity = entity;
    this.paramValue = paramValue;
    this.message = message;
  }

  public ForbiddenException() {
    message = null;
    paramValue = null;
    entity = null;
  }

    public ForbiddenException(Class<?> entity, String message) {
      super(message);
      this.entity = entity;
      this.paramValue = message;
      this.message = message;
    }
}
