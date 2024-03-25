package org.highmed.integrationtesting.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockNumUserSecurityContextFactory.class)
public @interface WithMockNumUser {

  String username() default "emmawoodhouse";

  String name() default "Emma Woodhouse";

  String email() default "emmawoodhouse@num.de";

  String[] roles() default {"USER"};

  String userId() default "b59e5edb-3121-4e0a-8ccb-af6798207a72";

  boolean expiredToken() default false;
}
