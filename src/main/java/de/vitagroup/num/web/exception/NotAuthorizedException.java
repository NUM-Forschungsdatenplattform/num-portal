package de.vitagroup.num.web.exception;

public class NotAuthorizedException extends RuntimeException {

  public NotAuthorizedException() {
    super();
  }

  public NotAuthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotAuthorizedException(String message) {
    super(message);
  }

  public NotAuthorizedException(Throwable cause) {
    super(cause);
  }
}
