package de.vitagroup.num.service.exception;

import lombok.Getter;

@Getter
public class ResourceNotFound extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String message;

  public ResourceNotFound(Class<?> entity, String message) {
    super(message);
    this.entity = entity;
    this.paramValue = message;
    this.message = message;
  }

  public ResourceNotFound(Class<?> entity, String paramValue, String message) {
    super(message);
    this.entity = entity;
    this.paramValue = paramValue;
    this.message = message;
  }

}
