package org.highmed.numportal.web.feign.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FeignInterceptor implements RequestInterceptor {

  private final TokenProvider tokenProvider;

  @Override
  public void apply(RequestTemplate requestTemplate) {
    final OAuth2AccessToken accessToken = tokenProvider.getAccessToken();
    requestTemplate.header(
        HttpHeaders.AUTHORIZATION,
        OAuth2AccessToken.TokenType.BEARER.getValue() + " " + accessToken.getTokenValue());
  }
}
