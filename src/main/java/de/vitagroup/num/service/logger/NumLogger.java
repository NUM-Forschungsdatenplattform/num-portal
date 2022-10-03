package de.vitagroup.num.service.logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@Aspect
@Component
public class NumLogger {

  private static final String LOG_TYPE = "AUDIT";

  private static final String POST = "POST";
  private static final String PUT = "PUT";
  private static final String DELETE = "DELETE";
  private static final String GET = "GET";

  @Before("@annotation(AuditLog)")
  public boolean logMethodCall(JoinPoint joinPoint) {

    try {
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        return true;
      }

      logApiOperations(
          joinPoint, (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    } catch (Exception e) {
      log.error("Cannot log audit log {}", e.getMessage());
    }
    return true;
  }

  private void logApiOperations(JoinPoint joinPoint, Jwt principal) {
    RequestMethod requestMethod = getRequestMethod(joinPoint);
    if (requestMethod == null) {
      return;
    }

    Class<?> clazz = joinPoint.getTarget().getClass();
    String url = getRequestUrl(joinPoint, requestMethod, clazz);

    Logger logger = LoggerFactory.getLogger(clazz);
    logger.info(
        String.format(
            "%s %s { userId: %s, %s: %s, payload: {%s}}",
            LOG_TYPE,
            OffsetDateTime.now(),
            principal.getSubject(),
            requestMethod,
            url,
            getPayload(joinPoint)));
  }

  private String getRequestUrl(JoinPoint joinPoint, RequestMethod requestMethod, Class<?> clazz) {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Method method = methodSignature.getMethod();
    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);

    switch (requestMethod.name()) {
      case POST:
        return getPostUrl(method, requestMapping);
      case GET:
        return getGetUrl(method, requestMapping);
      case PUT:
        return getPutUrl(method, requestMapping);
      case DELETE:
        return getDeleteUrl(method, requestMapping);
      default:
        return StringUtils.EMPTY;
    }
  }

  private String getPayload(JoinPoint joinPoint) {
    CodeSignature signature = (CodeSignature) joinPoint.getSignature();
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < joinPoint.getArgs().length; i++) {
      if (joinPoint.getArgs()[i] instanceof Jwt) {
        continue;
      }
      String parameterName = signature.getParameterNames()[i];
      Object parameterValue = joinPoint.getArgs()[i];

      builder.append(
          String.format(
              "%s: %s ",
              parameterName, parameterValue != null ? parameterValue.toString() : "null"));
    }

    return builder.toString();
  }

  private String getPostUrl(Method method, RequestMapping requestMapping) {
    PostMapping postMapping = method.getAnnotation(PostMapping.class);
    return String.format("%s%s", getUrl(requestMapping.value()), getUrl(postMapping.value()));
  }

  private String getPutUrl(Method method, RequestMapping requestMapping) {
    PutMapping putMapping = method.getAnnotation(PutMapping.class);
    return String.format("%s%s", getUrl(requestMapping.value()), getUrl(putMapping.value()));
  }

  private String getGetUrl(Method method, RequestMapping requestMapping) {
    GetMapping getMapping = method.getAnnotation(GetMapping.class);
    return String.format("%s%s", getUrl(requestMapping.value()), getUrl(getMapping.value()));
  }

  private String getDeleteUrl(Method method, RequestMapping requestMapping) {
    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
    return String.format("%s%s", getUrl(requestMapping.value()), getUrl(deleteMapping.value()));
  }

  private String getUrl(String[] urls) {
    if (urls.length == 0) {
      return StringUtils.EMPTY;
    } else {
      return urls[0];
    }
  }

  private RequestMethod getRequestMethod(JoinPoint point) {
    MethodSignature signature = (MethodSignature) point.getSignature();
    Method method = signature.getMethod();

    for (Annotation annotation : method.getDeclaredAnnotations()) {
      if (annotation
          .annotationType()
          .isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class)) {

        RequestMapping[] mappings =
            annotation
                .annotationType()
                .getAnnotationsByType(org.springframework.web.bind.annotation.RequestMapping.class);

        if (mappings.length > 0) {
          RequestMethod[] methods = mappings[0].method();
          if (methods.length > 0) {
            return methods[0];
          }
        }
      }
    }
    return null;
  }
}
