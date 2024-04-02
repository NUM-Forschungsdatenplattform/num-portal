package org.highmed.numportal.config;

import ca.uhn.fhir.context.FhirContext;
import org.highmed.numportal.properties.FttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FttpClientConfig {

  private static final String CERTIFICATE_TYPE = "PKCS12";

  private final FttpProperties fttpProperties;

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public CloseableHttpClient httpClient() {
    try {
      var keyStore = KeyStore.getInstance(CERTIFICATE_TYPE);

      keyStore.load(getClass().getResourceAsStream(fttpProperties.getCertificatePath()),
          fttpProperties.getCertificateKey().toCharArray());

      SSLContext sslContext =
          SSLContextBuilder.create().loadKeyMaterial(keyStore, fttpProperties.getCertificateKey().toCharArray()).build();

      HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
      SSLConnectionSocketFactory sslFactory =
          new SSLConnectionSocketFactory(sslContext, hostnameVerifier);

      CloseableHttpClient client;

      if (fttpProperties.isUseBasicAuth()) {
        var provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(fttpProperties.getUsername(), fttpProperties.getPassword())
        );

        client = HttpClients.custom().setSSLSocketFactory(sslFactory).setDefaultCredentialsProvider(provider).build();
      } else {
        client = HttpClients.custom().setSSLSocketFactory(sslFactory).build();
      }

      return client;
    } catch (Exception e) {
      log.error("Failed to create http client for fttp communication with cause [{}]", e.getLocalizedMessage());
    }
    return null;
  }
}
