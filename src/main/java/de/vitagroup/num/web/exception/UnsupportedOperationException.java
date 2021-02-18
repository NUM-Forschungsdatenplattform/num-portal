package de.vitagroup.num.web.exception;

public class UnsupportedOperationException extends RuntimeException{

  public UnsupportedOperationException() {
    super();
  }

  public UnsupportedOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedOperationException(String message) {
    super(message);
  }

}
