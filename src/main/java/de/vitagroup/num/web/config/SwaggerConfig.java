package de.vitagroup.num.web.config;

import de.vitagroup.num.properties.SwaggerProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class SwaggerConfig {
  private static final String SEC_CONFIG_NAME = "oauth_setting";

  private final SwaggerProperties swaggerProperties;

  @Bean
  public GroupedOpenApi profileApi() {
    return getDocket("Profile", "/profile/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi projectApi() {
    return getDocket("Project", "/project/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi templateApi() {
    return getDocket("Template", "/template/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi organizationApi() {
    return getDocket("Organization", "/organization/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi cohortApi() {
    return getDocket("Cohort", "/cohort/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi aqlApi() {
    return getDocket("Aql", "/aql/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi aqlEditorApi() {
    return getDocket("Aql editor", "/aqleditor/**", "org.ehrbase.aqleditor.controler");
  }

  @Bean
  public GroupedOpenApi adminApi() {
    return getDocket("Admin", "/admin/**", "de.vitagroup.num.web.controller");
  }

  @Bean
  public GroupedOpenApi contentApi() {
    return getDocket("Content", "/content/**", "de.vitagroup.num.web.controller");
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
