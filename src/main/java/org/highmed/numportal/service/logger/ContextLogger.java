package org.highmed.numportal.service.logger;

import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Optional;

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
            String id = getPathVariableValue(joinPoint);
            boolean printDto = contextLog.dtoPrint();
            Optional<Object> dto = Optional.empty();
            if (printDto) {
                dto = getDto(joinPoint);
                printDto = dto.isPresent();
            }
            logger.info(
                    "Operation: '{}', ID: '{}', DTO: '{}'", contextLog.description(),
                    id,
                    printDto?dto.get():"",
                    StructuredArguments.keyValue("type", type),
                    StructuredArguments.keyValue("loggedInUserId", userId)
            );
        } catch (Exception e) {
            logger.error("Cannot log context log {}", e);
        }
        return true;
    }

    private static String getPathVariableValue(JoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Annotation[][] array = ms.getMethod().getParameterAnnotations();
        for(int i = 0; i< array.length; i++){
            for(int j = 0; j< array[i].length; j++){
                if(array[i][j].annotationType().equals(org.springframework.web.bind.annotation.PathVariable.class)){
                    return joinPoint.getArgs()[i].toString();

                }
            }
        }
        return "";
    }

    private static Optional<Object> getDto(JoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Annotation[][] array = ms.getMethod().getParameterAnnotations();
        for(int i = 0; i< array.length; i++){
            for(int j = 0; j< array[i].length; j++){
                if(array[i][j].annotationType().equals(org.springframework.web.bind.annotation.RequestBody.class)){
                    return Optional.of(joinPoint.getArgs()[i]);

                }
            }
        }
        return Optional.empty();
    }
}

