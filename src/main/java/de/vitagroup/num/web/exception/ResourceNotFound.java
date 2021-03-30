package de.vitagroup.num.web.exception;

public class ResourceNotFound extends RuntimeException {

  public ResourceNotFound() {
    super();
  }

  public ResourceNotFound(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceNotFound(String message) {
    super(message);
  }

  public ResourceNotFound(Throwable cause) {
    super(cause);
  }
}
