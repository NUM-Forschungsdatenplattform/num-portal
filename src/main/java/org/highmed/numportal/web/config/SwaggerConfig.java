package org.highmed.numportal.web.config;

import org.highmed.numportal.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class SwaggerConfig {
  private static final String SEC_CONFIG_NAME = "oauth_setting";

  private static final String NUM_PACKAGES_TO_SCAN = "org.highmed.numportal.web.controller";


  private final SwaggerProperties swaggerProperties;

  @Bean
  public GroupedOpenApi profileApi() {
    return getDocket("Profile", "/profile/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi projectApi() {
    return getDocket("Project", "/project/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi templateApi() {
    return getDocket("Template", "/template/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi organizationApi() {
    return getDocket("Organization", "/organization/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi cohortApi() {
    return getDocket("Cohort", "/cohort/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi aqlApi() {
    return getDocket("Aql", "/aql/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi aqlEditorApi() {
    return getDocket("Aql editor", "/aqleditor/**", "org.ehrbase.aqleditor.controler");
  }

  @Bean
  public GroupedOpenApi adminApi() {
    return getDocket("Admin", "/admin/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi contentApi() {
    return getDocket("Content", "/content/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public GroupedOpenApi attachmentApi() {
    return getDocket("Attachment", "/attachment/**", NUM_PACKAGES_TO_SCAN);
  }

  @Bean
  public OpenAPI customOpenAPI() {
    OAuthFlow oAuthFlow = new OAuthFlow()
            .tokenUrl(swaggerProperties.getTokenUri())
            .authorizationUrl(swaggerProperties.getAuthUri());

    return new OpenAPI()
            .components(new Components()
                    .addSecuritySchemes("security_auth", new SecurityScheme()
                            .name(SEC_CONFIG_NAME)
                            .flows(new OAuthFlows().authorizationCode(oAuthFlow))
                            .type(SecurityScheme.Type.OAUTH2)
                            .scheme("oauth2")))
            .addSecurityItem(new SecurityRequirement().addList("security_auth"));

  }
  private GroupedOpenApi getDocket(String groupName, String pathRegexp, String... packagesToScan) {
    return GroupedOpenApi.builder()
            .group(groupName)
            .packagesToScan(packagesToScan)
            .pathsToMatch(pathRegexp)
            .build();
  }
}
