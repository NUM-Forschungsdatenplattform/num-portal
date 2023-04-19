package de.vitagroup.num.service.exception;

import de.vitagroup.num.service.exception.dto.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PASS_NOT_MATCHING;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.RECORD_ALREADY_EXISTS;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.RECORD_NOT_FOUND_MSG;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.TOKEN_IS_NOT_VALID_MSG;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USER_UNAUTHORISED_EXCEPTION;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.errorMap;
import static java.util.Objects.nonNull;

@Slf4j
public class CustomizedExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TokenIsNotValidException.class)
    public ResponseEntity<ErrorDetails> handleTokenErrors(
            TokenIsNotValidException exception) {

        var errors = Map.of( exception.getEntity().getSimpleName(),
                exception.getEntityId() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get( TOKEN_IS_NOT_VALID_MSG ).getId() )
                .argumentsList( errorMap.get( TOKEN_IS_NOT_VALID_MSG ).getArgumentsList() )
                .message( TOKEN_IS_NOT_VALID_MSG )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.badRequest().body( errorDetails );
    }

    @ExceptionHandler(UserUnauthorizedException.class)
    public ResponseEntity<ErrorDetails> handleUserErrors(
            UserUnauthorizedException exception) {

        var errors = Map.of( exception.getClass().getSimpleName(),
                exception.getMessage() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get( USER_UNAUTHORISED_EXCEPTION ).getId() )
                .argumentsList( Arrays.asList( exception.getUserId() ) )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.NOT_ACCEPTABLE ).body( errorDetails );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleEntityErrors(
            EntityNotFoundException exception) {

        var errors = Map.of( exception.getEntity().getSimpleName(),
                exception.getEntityId() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get(RECORD_NOT_FOUND_MSG ).getId() )
                .argumentsList( errorMap.get(RECORD_NOT_FOUND_MSG ).getArgumentsList() )
                .message(RECORD_NOT_FOUND_MSG )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.badRequest().body( errorDetails );
    }

    @ExceptionHandler(SameEntityExistsException.class)
    public ResponseEntity<ErrorDetails> handleSameEntityExistsErrors(
            SameEntityExistsException exception) {

        var errors = Map.of( exception.getEntity().getSimpleName(),
                exception.getParameter() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get( RECORD_ALREADY_EXISTS ).getId() )
                .argumentsList( errorMap.get( RECORD_ALREADY_EXISTS ).getArgumentsList() )
                .message( RECORD_ALREADY_EXISTS )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.CONFLICT ).body( errorDetails );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetails> handlePasswordMatchingErrors(
            BadCredentialsException exception) {

        var errors = Map.of( exception.getEntity().getSimpleName(),
                exception.getParameter() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get( PASS_NOT_MATCHING ).getId() )
                .argumentsList( errorMap.get( PASS_NOT_MATCHING ).getArgumentsList() )
                .message( PASS_NOT_MATCHING )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.badRequest().body( errorDetails );
    }

    @ExceptionHandler(UsernameNotFoundOrNoLongerActiveException.class)
    public ResponseEntity<ErrorDetails> handleUsernameNotFoundOrNoLongerActiveErrors(
            UsernameNotFoundOrNoLongerActiveException exception) {

        var errors = Map.of( exception.getEntity().getSimpleName(),
                exception.getParameter() );

        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( errorMap.get( USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE ).getId() )
                .argumentsList( errorMap.get( USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE ).getArgumentsList() )
                .message( USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.NOT_ACCEPTABLE ).body( errorDetails );
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorDetails> handleCustomErrors(
            CustomException exception) {

        var errors = Map.of( "Error Message", exception.getMessage() );
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getMessage())) ? errorMap.get( exception.getMessage() ).getId() : -1)
                .argumentsList( new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( errorDetails );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestErrors(
            BadRequestException exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( errorDetails );
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorDetails> handleForbiddenErrors(
            ForbiddenException exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return new ResponseEntity<>(errorDetails, new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PrivacyException.class)
    public ResponseEntity<ErrorDetails> handlePrivacyErrors(
            PrivacyException exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS ).body( errorDetails );
    }

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundErrors(
            ResourceNotFound exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.NOT_FOUND ).body( errorDetails );
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorDetails> handleSystemException(
            SystemException exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( errorDetails );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        var className = nonNull(exception.getEntity()) ? exception.getEntity().getSimpleName() : null;
        var description = exception.getMessage();

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : description);
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getParamValue())) ? errorMap.get( exception.getParamValue() ).getId() : -1)
                .argumentsList( nonNull(exception.getEntity()) ? Arrays.asList(className, description) : new ArrayList<>() )
                .message( exception.getMessage() )
                .details( errors )
                .build();
        log.debug(exception.getMessage(), exception);
        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( errorDetails );
    }

    @ExceptionHandler({WrongStatusCodeException.class})
    public ResponseEntity<ErrorDetails> handleWrongStatus(WrongStatusCodeException exception) {
        String actualStatusCode = String.valueOf(exception.getActualStatusCode());
        String expectedStatusCode = String.valueOf(exception.getExpectedStatusCode());

        var errors = Map.of( "Error Message",
                nonNull(exception.getMessage()) ? exception.getMessage() : "WrongStatusCodeException.class");
        ErrorDetails errorDetails = ErrorDetails
                .builder()
                .messageId( nonNull(errorMap.get( exception.getMessage())) ? errorMap.get( exception.getMessage() ).getId() : -1)
                .argumentsList( Arrays.asList(actualStatusCode, expectedStatusCode)  )
                .message( exception.getMessage() )
                .details( errors )
                .build();

        log.error(exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    // TODO test behaviour and find replacement if is the case because of deprecated handleBindException
    // as of 6.0 since ModelAttributeMethodProcessor now raises the MethodArgumentNotValidException subclass instead.
//    protected ResponseEntity<Object> handleBindException(BindException exception, HttpHeaders headers,
//                                                         HttpStatus status, WebRequest request) {
//
//        Map<String, String> errors = new HashMap<>();
//        exception.getFieldErrors()
//                .forEach( error -> errors.put( error.getField(), error.getDefaultMessage() ) );
//
//        ErrorDetails errorDetails = ErrorDetails
//                .builder()
//                .message( "Error" )
//                .details( errors )
//                .build();
//        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( errorDetails );
//    }
}
