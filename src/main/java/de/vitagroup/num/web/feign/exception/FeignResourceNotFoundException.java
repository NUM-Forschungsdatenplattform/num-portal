package de.vitagroup.num.web.feign.exception;

public class FeignResourceNotFoundException extends RuntimeException{

    public FeignResourceNotFoundException() {
        super();
    }

    public FeignResourceNotFoundException(String message) {
        super(message);
    }

}
