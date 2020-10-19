package de.vitagroup.num.config;

import lombok.Data;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak-admin")
public class KeycloakConfig {
    private String authServerUrl;
    private String realm;
    private String client;
    private String secret;

    @Bean
    public Keycloak keycloak() {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(client)
                .clientSecret(secret)
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build())
                .build();

        return keycloak;
    }
}
