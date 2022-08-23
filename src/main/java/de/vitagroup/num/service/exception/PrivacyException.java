package de.vitagroup.num.service.exception;

import lombok.Getter;

@Getter
public class PrivacyException extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String parameter;

  public PrivacyException(Class<?> entity, String parameter) {
    super(parameter);
    this.entity = entity;
    this.paramValue = parameter;
    this.parameter = parameter;
  }

/*  public PrivacyException(Class<?> entity, String paramValue, String parameter) {
    super(parameter);
    this.entity = entity;
    this.paramValue = paramValue;
    this.parameter = parameter;
  }*/


}
