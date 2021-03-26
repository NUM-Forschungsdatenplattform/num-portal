package de.vitagroup.num.web.config;

import de.vitagroup.num.properties.CorsProperties;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

  private static final String BASE_PATH = "/**";

  private final CorsProperties corsProperties;

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration corsConfiguration = new CorsConfiguration();

    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
    corsConfiguration.setAllowedMethods(Collections.singletonList(CorsConfiguration.ALL));
    corsConfiguration.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
    source.registerCorsConfiguration(BASE_PATH, corsConfiguration);
    return new CorsFilter(source);
  }
}
