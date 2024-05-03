package org.highmed.numportal.service.html;

import com.ctc.wstx.shaded.msv_core.util.Uri;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@Log4j2
public class HtmlContent {
    private CloseableHttpClient httpClient = null;
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0";

    @PostConstruct
    public void init() throws Exception {
        httpClient = createTrustAllHttpClientBuilder().build();
    }

	private HttpClientBuilder createTrustAllHttpClientBuilder() throws Exception {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial( null, (chain, authType) -> true );
        SSLConnectionSocketFactory sslsf = new
                SSLConnectionSocketFactory( builder.build(), NoopHostnameVerifier.INSTANCE );

        int timeout = 5;
        RequestConfig config = getRequestConfig( timeout );

        return HttpClients.custom().setSSLSocketFactory( sslsf ).setUserAgent( USER_AGENT ).setDefaultRequestConfig( config );
    }

    @PreDestroy
    public void destroy() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    private static RequestConfig getRequestConfig(int timeout) {
        return RequestConfig.custom()
                .setConnectTimeout( timeout * 1000 )
                .setConnectionRequestTimeout( timeout * 1000 )
                .setSocketTimeout( timeout * 1000 ).build();
    }

    public String pageContent(String urlStr) throws IOException, URISyntaxException {

        CloseableHttpResponse response = null;
        try {
            isValidURI(urlStr);
            URI uri = new URI( urlStr );
            HttpGet request = new HttpGet( uri );
            request.setConfig( getRequestConfig( 5*5 ) );

            response = httpClient.execute( request );

            if (response.getStatusLine().getStatusCode() > 200) {
                throw new RuntimeException( "Invalid response for url: " + urlStr );
            }

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            ContentType contentType = ContentType.get( entity );
            Charset charSet = StandardCharsets.UTF_8;

            if (contentType != null) {
                charSet = contentType.getCharset();

            } else {
                log.info( "contentType unknown" );
            }

            //file content
            return IOUtils.toString( inputStream, charSet != null ? charSet : StandardCharsets.ISO_8859_1 );

        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
    private boolean isValidURI(String uriString){
        Boolean statsusCakeUrl = uriString.contains("statusCake");
        Boolean systemStatusUrl = uriString.contains("codex");
        return statsusCakeUrl || systemStatusUrl;
    }

}
