package de.vitagroup.num.web.exception;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {

    log.error("Http message not readable", ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(List.of("Http message not readable")).build();
    return new ResponseEntity<>(response, headers, HttpStatus.BAD_REQUEST);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.toList());

    ErrorResponse response = ErrorResponse.builder().errors(errors).build();
    return new ResponseEntity<>(response, headers, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({Exception.class, SystemException.class})
  public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
    log.error(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder()
            .errors(Collections.singletonList("An unexpected error has occurred"))
            .build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class})
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
    log.debug(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({WrongStatusCodeException.class})
  public ResponseEntity<ErrorResponse> handleWrongStatus(WrongStatusCodeException ex) {
    log.error(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ResourceNotFound.class})
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFound ex) {
    log.error(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({ConflictException.class})
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    log.error(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> handleNotApprovedUser(Exception ex) {
    log.debug(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler({PrivacyException.class})
  public ResponseEntity<ErrorResponse> handlePrivacyException(PrivacyException ex) {
    log.debug(ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder().errors(Collections.singletonList(ex.getMessage())).build();
    return new ResponseEntity<>(
        response, new HttpHeaders(), HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
  }
}
