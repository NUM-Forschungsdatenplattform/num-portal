package org.highmed.service.exception.implementation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SameEntityExistsException extends RuntimeException  {

  private final Class<?> entity;
  private final String parameter;
}
