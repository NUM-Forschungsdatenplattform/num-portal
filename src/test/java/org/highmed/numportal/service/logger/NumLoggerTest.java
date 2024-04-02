package org.highmed.numportal.service.logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.highmed.numportal.service.logger.AuditLog;
import org.highmed.numportal.service.logger.NumLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NumLoggerTest {

    @InjectMocks
    private NumLogger numLogger;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private AuditLog auditLog;

    @Test(expected = Exception.class)
    public void logMethodCallException() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new BearerTokenAuthenticationToken(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
        SecurityContextHolder.setContext(context);
        String exception = "Cannot log audit log class java.lang.String cannot be cast to class org.springframework.security.oauth2.jwt.Jwt (java.lang.String is in module java.base of loader 'bootstrap'; org.springframework.security.oauth2.jwt.Jwt is in unnamed module of loader 'app')";
        when(numLogger.logMethodCall(joinPoint, auditLog))
                .thenThrow(new Exception(exception));
        assertTrue(numLogger.logMethodCall(joinPoint, auditLog));
    }

    @Test
    public void logMethodCallAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);
        boolean result = numLogger.logMethodCall(joinPoint, auditLog);
        assertFalse(result);
    }

    @Test
    public void logMethodCallReturnTrue() throws NoSuchMethodException {
        Jwt jwt = Jwt.withTokenValue("12345")
                .subject("user-uuid-12345")
                .issuedAt(Instant.now())
                .claim("name", "John")
                .claim("email", "john.doe@vitagroup.de")
                .claim("username", "john.doe")
                .header("dummy", "dummy")
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, Collections.emptySet()));
        SecurityContextHolder.setContext(context);
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(testMethod());
        boolean result = numLogger.logMethodCall(joinPoint, auditLog);
        assertTrue(result);
        Mockito.verify(joinPoint, Mockito.times(1)).getSignature();
    }
    private Method testMethod() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("someTestMethod");
    }
    private void someTestMethod() {
    }

}
