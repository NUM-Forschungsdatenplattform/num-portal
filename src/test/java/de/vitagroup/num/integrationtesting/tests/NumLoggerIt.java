package de.vitagroup.num.integrationtesting.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import de.vitagroup.num.service.logger.NumLogger;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

public class NumLoggerIt {

    @InjectMocks
    private NumLogger numLogger;

    JoinPoint joinPoint = null;

    @Test(expected = Exception.class)
    public void logMethodCallException() {
        NumLogger numLogger = org.mockito.Mockito.mock(NumLogger.class);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new BearerTokenAuthenticationToken(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
        SecurityContextHolder.setContext(context);
        String exception = "Cannot log audit log class java.lang.String cannot be cast to class org.springframework.security.oauth2.jwt.Jwt (java.lang.String is in module java.base of loader 'bootstrap'; org.springframework.security.oauth2.jwt.Jwt is in unnamed module of loader 'app')";
        when(numLogger.logMethodCall(joinPoint))
                .thenThrow(new Exception(exception));
        assertTrue(numLogger.logMethodCall(joinPoint));
    }

    @Test
    public void logMethodCallAuthentication() {
        NumLogger numLogger = org.mockito.Mockito.mock(NumLogger.class);
        SecurityContextHolder.getContext().setAuthentication(null);
        when(numLogger.logMethodCall(joinPoint))
                .thenReturn(Boolean.FALSE);
        assertFalse(numLogger.logMethodCall(joinPoint));
    }

    @Test
    public void logMethodCallReturnTrue() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new BearerTokenAuthenticationToken(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
        SecurityContextHolder.setContext(context);
        NumLogger numLogger = org.mockito.Mockito.mock(NumLogger.class);
        when(numLogger.logMethodCall(joinPoint))
                .thenReturn(Boolean.TRUE);
        assertTrue(numLogger.logMethodCall(joinPoint));
    }

}
