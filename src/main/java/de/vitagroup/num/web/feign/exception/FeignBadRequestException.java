package de.vitagroup.num.web.feign.exception;

import feign.FeignException;

public class FeignBadRequestException extends RuntimeException {

  public FeignBadRequestException() {
    super();
  }

  public FeignBadRequestException(String message) {
    super(message);
  }
}
