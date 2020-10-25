package de.vitagroup.num.web.feign.config;

import feign.Client;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

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

  // ---------------------------- CODE BELOW ONLY A HACK TO GET PAST SELF SIGNED CERTIFICATE IN
  // TEMPORARY KEYCLOAK
  private static final HostnameVerifier jvmHostnameVerifier =
      HttpsURLConnection.getDefaultHostnameVerifier();

  private static final HostnameVerifier trivialHostnameVerifier =
      new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession sslSession) {
          return true;
        }
      };

  private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER =
      new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {}

          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
      };

  public static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException {
    HttpsURLConnection.setDefaultHostnameVerifier(trivialHostnameVerifier);
    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }

  @Bean
  public Client feignClient() {
    try {
      OAuth2Configuration.turnOffSslChecking();
    } catch (Exception e) {
      // do nothing
    }
    return new Client.Default(getSSLSocketFactory(), new NoopHostnameVerifier());
  }

  private SSLSocketFactory getSSLSocketFactory() {
    try {
      SSLContext sslContext =
          SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
      return sslContext.getSocketFactory();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
