package de.vitagroup.num.config;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ehrbase.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EhrBaseConfig {

    @Bean
    public DefaultRestClient createRestClient() throws URISyntaxException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials("ehrbase-user", "SuperSecretPassword"));

        CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .build();

        return new DefaultRestClient(
            new OpenEhrClientConfig(new URI("http://localhost:8080/ehrbase/rest/openehr/v1/")),
            new EhrTemplateProvider(), httpClient);
    }
}
