package de.vitagroup.num.web.exception;

public class SystemException extends RuntimeException {

  public SystemException() {
    super();
  }

  public SystemException(String message, Throwable cause) {
    super(message, cause);
  }

  public SystemException(String message) {
    super(message);
  }

  public SystemException(Throwable cause) {
    super(cause);
  }
}
