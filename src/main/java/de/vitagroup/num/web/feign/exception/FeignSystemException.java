package de.vitagroup.num.web.feign.exception;

public class FeignSystemException extends RuntimeException {

  public FeignSystemException() {
    super();
  }

  public FeignSystemException(String message) {
    super(message);
  }
}
