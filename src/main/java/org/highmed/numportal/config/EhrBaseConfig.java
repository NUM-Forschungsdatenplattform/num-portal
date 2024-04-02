package org.highmed.numportal.config;

import org.highmed.numportal.properties.EhrBaseProperties;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@RequiredArgsConstructor
public class EhrBaseConfig {

  private final EhrBaseProperties ehrBaseProperties;

  @Bean
  @Primary
  public DefaultRestClient createRestClient() throws URISyntaxException {
    CredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(
            ehrBaseProperties.getUsername(), ehrBaseProperties.getPassword()));

    CloseableHttpClient httpClient =
        HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

    return new DefaultRestClient(
        new OpenEhrClientConfig(new URI(ehrBaseProperties.getRestApiUrl())), null, httpClient);
  }
}
