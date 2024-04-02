package org.highmed.numportal.service.exception;

import org.highmed.numportal.domain.model.admin.UserDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Log4j2
@Getter
public class UserUnauthorizedException extends RuntimeException {

    UserDetails userDetails;
    String userId;
    String message;

    public UserUnauthorizedException(UserDetails userDetails) {
        super("User " + userDetails.getUserId() + " is not authorized!");
        this.userDetails = userDetails;
        this.userId = userDetails.getUserId();
        log.info("User: {} is not authorized!", userDetails.getUserId());
    }

    public UserUnauthorizedException(String message) {
        super(message);
        this.message = message;
        this.userId = userDetails.getUserId();
    }

}
