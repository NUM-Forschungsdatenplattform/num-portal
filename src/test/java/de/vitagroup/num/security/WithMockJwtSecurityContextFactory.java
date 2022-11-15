package de.vitagroup.num.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.StringUtils;

public class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwt> {

  @Override
  public SecurityContext createSecurityContext(WithMockJwt withMockJwt) {
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    Jwt jwt = Jwt.withTokenValue("123456789")
        .subject(withMockJwt.preferredUsername())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
        .claim("realm_access", Map.of("roles", StringUtils.commaDelimitedListToSet(withMockJwt.realmAccessRoles())))
        .claim("preferred_username", withMockJwt.preferredUsername())
        .header("fake", "fake")
        .build();
    Set<String> roles = StringUtils.commaDelimitedListToSet(withMockJwt.realmAccessRoles());
    Set<GrantedAuthority> authorities = roles.stream().map(r -> "ROLE_" + r).map(r -> new SimpleGrantedAuthority(r)).collect(Collectors.toSet());


    JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt, authorities);
    securityContext.setAuthentication(jwtAuthenticationToken);
    return securityContext;
  }

  protected Map<String,Object> buildClaims(WithMockJwt withMockJwt) {
    return Map.of("realm_access", Map.of("roles", StringUtils.commaDelimitedListToSet(withMockJwt.realmAccessRoles())),"preferred_username", withMockJwt.preferredUsername(), "bsnr", "987654321" );
  }
}
