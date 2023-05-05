package de.vitagroup.num.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApplicationSecurity {

  private static final String[] AUTH_WHITELIST = {"/swagger-*/**", "/v2/**", "/v3/**", "/admin/health",
          "/admin/log-level", "/admin/log-level/*"};

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
            .httpBasic().disable()
            .formLogin().disable()
            .authorizeRequests()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(new AuthorizationConverter())
                .and()
            .and()
            .sessionManagement(sessionManagement ->
                               sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors();
    return httpSecurity.build();
  }

  @Bean
  @Order(0)
  SecurityFilterChain whitelisted(HttpSecurity http) throws Exception {
          http
            .requestMatchers((matchers) -> matchers
                  .antMatchers(AUTH_WHITELIST)
                  .mvcMatchers(HttpMethod.GET, "/content/navigation")
                  .mvcMatchers(HttpMethod.GET, "/content/cards")
                  .mvcMatchers(HttpMethod.GET, "/content/metrics")
                  .mvcMatchers(HttpMethod.GET, "/actuator/health/**")
            .mvcMatchers(HttpMethod.GET, "/actuator/info**"))
            .authorizeRequests().anyRequest().permitAll()
                  .and()
                  .sessionManagement().disable()
                  .securityContext().disable();
    return http.build();
  }
}
