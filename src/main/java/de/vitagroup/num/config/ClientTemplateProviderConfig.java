package de.vitagroup.num.config;

import lombok.RequiredArgsConstructor;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.client.templateprovider.ClientTemplateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ClientTemplateProviderConfig {

  private final DefaultRestClient defaultRestClient;

  @Bean
  public ClientTemplateProvider createClietTemplateProvider() {
    return new ClientTemplateProvider(defaultRestClient);
  }
}
