package de.vitagroup.num.service.exception;

import lombok.Getter;

@Getter
public class ResourceNotFound extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String parameter;

  public ResourceNotFound(Class<?> entity, String parameter) {
    super(parameter);
    this.entity = entity;
    this.paramValue = parameter;
    this.parameter = parameter;
  }

  public ResourceNotFound(Class<?> entity, String paramValue, String parameter) {
    super(parameter);
    this.entity = entity;
    this.paramValue = paramValue;
    this.parameter = parameter;
  }

}
