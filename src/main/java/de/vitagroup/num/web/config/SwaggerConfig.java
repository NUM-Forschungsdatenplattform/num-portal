package de.vitagroup.num.web.config;

import de.vitagroup.num.properties.SwaggerProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.AuthorizationCodeGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@AllArgsConstructor
public class SwaggerConfig {
  private static final String SEC_CONFIG_NAME = "oauth_setting";

  private final SwaggerProperties swaggerProperties;

  @Bean
  public Docket profileApi() {
    return getDocket("Profile", "/profile.*");
  }

  @Bean
  public Docket studyApi() {
    return getDocket("Study", "/study.*");
  }

  @Bean
  public Docket templateApi() {
    return getDocket("Template", "/template.*");
  }

  @Bean
  public Docket organizationApi() {
    return getDocket("Organization", "/organization.*");
  }

  @Bean
  public Docket cohortApi() {
    return getDocket("Cohort", "/cohort.*");
  }

  @Bean
  public Docket phenotypeApi() {
    return getDocket("Phenotype", "/phenotype.*").useDefaultResponseMessages(false);
  }

  @Bean
  public Docket aqlApi() {
    return getDocket("Aql", "/aql.*", "org.ehrbase.aqleditor");
  }

  @Bean
  public Docket aqlEditorApi() {
    return getDocket("Aql editor", "/aqleditor.*");
  }

  @Bean
  public Docket adminApi() {
    return getDocket("Admin", "/admin.*");
  }

  @Bean
  public SecurityConfiguration security(SwaggerProperties properties) {
    return SecurityConfigurationBuilder.builder()
        .clientId(properties.getClientName())
        .clientSecret(properties.getClientSecret())
        .scopeSeparator(" ")
        .useBasicAuthenticationWithAccessCodeGrant(true)
        .build();
  }

  private Docket getDocket(String groupName, String pathRegexp, String... excludedBasePackage) {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName(groupName)
        .select()
        .apis(getRequestHandlerSelector(excludedBasePackage))
        .paths(PathSelectors.regex(pathRegexp))
        .build()
        .securitySchemes(Collections.singletonList(securityScheme()))
        .securityContexts(Collections.singletonList(securityContext(pathRegexp)));
  }

  private Predicate<RequestHandler> getRequestHandlerSelector(String... excludedBasePackage) {
    if (excludedBasePackage.length > 0) {
      return Predicate.not(RequestHandlerSelectors.basePackage(excludedBasePackage[0]));
    }
    return RequestHandlerSelectors.any();
  }

  private SecurityScheme securityScheme() {
    GrantType grantType =
        new AuthorizationCodeGrantBuilder()
            .tokenEndpoint(
                tokenEndpointBuilder ->
                    tokenEndpointBuilder
                        .url(swaggerProperties.getTokenUri())
                        .tokenName("oauthtoken"))
            .tokenRequestEndpoint(
                tokenRequestEndpointBuilder ->
                    tokenRequestEndpointBuilder
                        .url(swaggerProperties.getAuthUri())
                        .clientIdName(swaggerProperties.getClientName())
                        .clientSecretName(swaggerProperties.getClientSecret()))
            .build();

    return new OAuthBuilder()
        .name(SEC_CONFIG_NAME)
        .grantTypes(Collections.singletonList(grantType))
        .scopes(Arrays.asList(scopes()))
        .build();
  }

  private AuthorizationScope[] scopes() {
    return new AuthorizationScope[] {};
  }

  private SecurityContext securityContext(String pathRegexp) {
    return SecurityContext.builder()
        .securityReferences(
            Collections.singletonList(new SecurityReference(SEC_CONFIG_NAME, scopes())))
        .operationSelector(
            operationContext -> operationContext.requestMappingPattern().matches(pathRegexp))
        .build();
  }
}
