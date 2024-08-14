package org.highmed.numportal.service.logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class ContextLogger {

    private static final Logger logger = LoggerFactory.getLogger(ContextLogger.class);

    @Before("@annotation(contextLog)")
    public void logBefore(JoinPoint joinPoint, ContextLog contextLog) {
        String type = contextLog.type();
        String operation = contextLog.operation();
        String description = contextLog.description();

        logger.info("Entering method: {} with context: type={}, operation={}, description={}",
                joinPoint.getSignature().toShortString(), type, operation, description);
    }

    @AfterReturning(pointcut = "@annotation(contextLog)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, ContextLog contextLog, Object result) {
        String type = contextLog.type();
        String operation = contextLog.operation();
        String description = contextLog.description();

        logger.info("Exiting method: {} with context: type={}, operation={}, description={} and result: {}",
                joinPoint.getSignature().toShortString(), type, operation, description, result);
    }
}

