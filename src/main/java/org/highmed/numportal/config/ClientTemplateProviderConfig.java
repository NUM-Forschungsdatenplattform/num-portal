package org.highmed.numportal.config;

import lombok.RequiredArgsConstructor;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.templateprovider.ClientTemplateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ClientTemplateProviderConfig {

  private final DefaultRestClient defaultRestClient;

  @Bean
  public ClientTemplateProvider createClientTemplateProvider() {
    return new ClientTemplateProvider(defaultRestClient);
  }
}
