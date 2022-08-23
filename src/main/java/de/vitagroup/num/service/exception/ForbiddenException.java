package de.vitagroup.num.service.exception;

import de.vitagroup.num.service.ProjectService;
import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String parameter;

  public ForbiddenException(Class<?> entity, String paramValue, String parameter) {
    super(parameter);
    this.entity = entity;
    this.paramValue = paramValue;
    this.parameter = parameter;
  }

  public ForbiddenException() {
    parameter = null;
    paramValue = null;
    entity = null;
  }

    public ForbiddenException(Class<?> entity, String parameter) {
      super(parameter);
      this.entity = entity;
      this.paramValue = parameter;
      this.parameter = parameter;
    }
}
