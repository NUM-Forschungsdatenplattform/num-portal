package de.vitagroup.num.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ApplicationSecurity extends WebSecurityConfigurerAdapter {

  private static final String[] AUTH_WHITELIST = {"/swagger-*/**", "/v2/**", "/v3/**", "/admin/health",
          "/admin/log-level", "/admin/log-level/*"};

  @Override
  public void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .httpBasic()
        .disable()
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors()
        .and()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(new AuthorizationConverter());
  }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers(AUTH_WHITELIST);
    web.ignoring().mvcMatchers(HttpMethod.GET, "/content/navigation");
    web.ignoring().mvcMatchers(HttpMethod.GET, "/content/cards");
    web.ignoring().mvcMatchers(HttpMethod.GET, "/content/metrics");
    web.ignoring().mvcMatchers(HttpMethod.GET, "/actuator/health/**");
    web.ignoring().mvcMatchers(HttpMethod.GET, "/actuator/info**");
  }
}
