package org.highmed.numportal.web.config;

import org.highmed.numportal.web.feign.KeycloakFeign;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = KeycloakFeign.class)
public class FeignClientConfig {
}
