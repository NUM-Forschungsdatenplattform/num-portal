package org.highmed.numportal.service.logger;

import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class ContextLogger {

    private static final Logger logger = LoggerFactory.getLogger(ContextLogger.class);

    @Before("@annotation(contextLog)")
    public boolean logBefore(JoinPoint joinPoint, ContextLog contextLog) {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }
        try {
            String type = contextLog.type();
            Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userId = principal.getSubject();
            logger.info("Logging context",
                    StructuredArguments.keyValue("method", joinPoint.getSignature().toShortString()),
                    StructuredArguments.keyValue("type", type),
                    StructuredArguments.keyValue("userID", userId)
            );
        } catch (Exception e) {
            logger.error("Cannot log context log {}", e);
        }
        return true;
    }
}

