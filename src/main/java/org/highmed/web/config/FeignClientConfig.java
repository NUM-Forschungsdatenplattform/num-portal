package org.highmed.web.config;

import org.highmed.web.feign.KeycloakFeign;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = KeycloakFeign.class)
public class FeignClientConfig {
}
