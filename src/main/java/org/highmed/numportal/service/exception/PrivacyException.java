package org.highmed.numportal.service.exception;

import lombok.Getter;

@Getter
public class PrivacyException extends RuntimeException {

  private final Class<?> entity;

  private final String paramValue;

  private final String message;

  public PrivacyException(Class<?> entity, String message) {
    super(message);
    this.entity = entity;
    this.paramValue = message;
    this.message = message;
  }
}
