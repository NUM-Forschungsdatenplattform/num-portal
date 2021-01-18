package de.vitagroup.num.web.exception;

public class NotAuthorizedException extends RuntimeException {

  public NotAuthorizedException() {
    super();
  }

  public NotAuthorizedException(String message) {
    super(message);
  }
}
