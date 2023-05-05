package de.vitagroup.num.integrationtesting.security;

import lombok.SneakyThrows;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WithMockNumUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockNumUser> {

  @SneakyThrows
  @Override
  public SecurityContext createSecurityContext(WithMockNumUser numUser) {

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    Instant issuedAt = Instant.now();
    Instant exp = numUser.expiredToken() ? issuedAt.plusNanos(1) : Instant.now().plusSeconds(60L);
    Jwt jwt = Jwt.withTokenValue("1111")
            .subject(numUser.userId())
            .issuedAt(issuedAt)
            .expiresAt(exp)
            .claim("name", numUser.name())
            .claim("email", numUser.email())
            .claim("realm_access", Map.of("roles", Arrays.asList(numUser.roles())))
            .claim("username", numUser.username())
            .header("dummy", "dummy")
            .build();
    Set<GrantedAuthority> authorities = Arrays.stream(numUser.roles()).map(r -> "ROLE_" + r).map(r -> new SimpleGrantedAuthority(r)).collect(Collectors.toSet());

    context.setAuthentication( new JwtAuthenticationToken(jwt, authorities));

    return context;
  }
}
