package de.vitagroup.num.web.config;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class FeignExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(FeignException.class)
    public final ResponseEntity<String> handleAllExceptions(FeignException ex, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if(status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(ex.getLocalizedMessage(), status);
    }
}
