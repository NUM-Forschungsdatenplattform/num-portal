package de.vitagroup.num.web.feign.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2Configuration {

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(OAuth2ClientProperties properties) {

    OAuth2AuthorizedClientProvider authorizedClientProvider =
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();

    List<ClientRegistration> registrations =
        new ArrayList<>(
            OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());

    InMemoryClientRegistrationRepository clientRegistrationRepository =
        new InMemoryClientRegistrationRepository(registrations);

    AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository));
    manager.setAuthorizedClientProvider(authorizedClientProvider);

    return manager;
  }
}
