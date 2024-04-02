package org.highmed.service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TokenIsNotValidException extends RuntimeException  {

  private final Class<?> entity;
  private final String entityId;
}
