package de.vitagroup.num.security;

import de.vitagroup.num.domain.Roles;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory.class)
public @interface WithMockJwt {

  String preferredUsername() default "pmeier";
  String realmAccessRoles() default Roles.MANAGER;
}


