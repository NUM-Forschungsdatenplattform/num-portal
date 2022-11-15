package de.vitagroup.num.web.config;

import de.vitagroup.num.web.feign.KeycloakFeign;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = KeycloakFeign.class)
public class FeignConfig {
}
