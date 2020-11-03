package de.vitagroup.num.web.feign.config;

import feign.Client;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
  private KeyStore getKeyStore()
      throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {
    CertificateFactory fact = CertificateFactory.getInstance("X.509");
    InputStream is = OAuth2Configuration.class.getResourceAsStream("/temp-keycloak.pem");
    X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null);
    keystore.setCertificateEntry("temp", cer);
    return keystore;
  }

  private TrustManager[] getTrustmanagers(KeyStore extraStore)
      throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory extraTrustManagerFactory = TrustManagerFactory.getInstance("X509");
    extraTrustManagerFactory.init(extraStore);
    TrustManager[] extraTrustManagers = extraTrustManagerFactory.getTrustManagers();
    TrustManagerFactory defaultTrustManagerFactory = TrustManagerFactory.getInstance("X509");
    defaultTrustManagerFactory.init((KeyStore) null);
    TrustManager[] defaultTrustManagers = defaultTrustManagerFactory.getTrustManagers();
    List<TrustManager> trustManagerList = pruneTrustmanagers(extraTrustManagers);
    trustManagerList.addAll(pruneTrustmanagers(defaultTrustManagers));
    return trustManagerList.toArray(new TrustManager[] {});
  }

  private List<TrustManager> pruneTrustmanagers(TrustManager[] trustManagers) {
    return Arrays.stream(trustManagers)
        .filter(trustManager -> trustManager instanceof X509TrustManager)
        .collect(Collectors.toList());
  }

  public void setupTrustManagers()
      throws NoSuchAlgorithmException, KeyManagementException, CertificateException,
          KeyStoreException, IOException {
    // Install the trust manager
    KeyStore keyStore = getKeyStore();
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, getTrustmanagers(keyStore), null);
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }

  @Bean
  public Client feignClient() {
    try {
      setupTrustManagers();
    } catch (Exception e) {
      log.error("trustmanager setup failed", e);
    }
    return new Client.Default(
        HttpsURLConnection.getDefaultSSLSocketFactory(),
        HttpsURLConnection.getDefaultHostnameVerifier());
  }
}
