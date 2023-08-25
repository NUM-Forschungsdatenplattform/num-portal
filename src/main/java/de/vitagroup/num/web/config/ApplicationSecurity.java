package de.vitagroup.num.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class ApplicationSecurity {

  private static final String[] AUTH_WHITELIST = {"/swagger-*/**", "/v2/**", "/v3/**", "/admin/health",
          "/admin/log-level", "/admin/log-level/*", "/admin/status-url"};

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
            .httpBasic().disable()
            .formLogin().disable()
            .authorizeHttpRequests()
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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) ->
                web.ignoring()
                        .requestMatchers(AUTH_WHITELIST)
                        .requestMatchers(HttpMethod.GET, "/content/navigation")
                        .requestMatchers(HttpMethod.GET, "/content/cards")
                        .requestMatchers(HttpMethod.GET, "/content/metrics")
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**")
                        .requestMatchers(HttpMethod.GET, "/actuator/info**");


    }
}
