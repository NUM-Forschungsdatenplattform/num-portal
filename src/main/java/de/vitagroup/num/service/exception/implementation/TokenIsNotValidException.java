package de.vitagroup.num.service.exception.implementation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TokenIsNotValidException extends RuntimeException  {

  private final Class<?> entity;
  private final String entityId;
}
