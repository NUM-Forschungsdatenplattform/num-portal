package de.vitagroup.num.web.config;

import de.vitagroup.num.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApplicationSecurity extends WebSecurityConfigurerAdapter {

  private static final String[] AUTH_WHITELIST = {"/swagger-*/**", "/v2/**", "/v3/**"};
  private static final String BASE_PATH = "/**";

  private final CorsProperties corsProperties;

  @Override
  public void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .httpBasic()
        .disable()
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors()
        .configurationSource(corsConfigurationSource());
  }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers(AUTH_WHITELIST);
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.applyPermitDefaultValues();
    corsConfiguration.setAllowedOrigins(corsProperties.getAllowedOrigins());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(BASE_PATH, corsConfiguration);

    return source;
  }
}
