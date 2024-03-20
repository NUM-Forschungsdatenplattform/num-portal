package org.highmed.web.feign.config;

import java.util.Collection;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ClientTokenProvider implements TokenProvider {

  private static final String CLIENT_REGISTRATION_ID = "userStoreClient";

  private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

  @Override
  public OAuth2AccessToken getAccessToken() {

    OAuth2AuthorizedClient auth2AuthorizedClient =
        oAuth2AuthorizedClientManager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(new ClientAuthentication())
                .build());

    if (auth2AuthorizedClient == null) {
      return null;
    }
    return auth2AuthorizedClient.getAccessToken();
  }

  /** Principal not used but needed for creation of the OAuth2AuthorizeRequest; only name used */
  private class ClientAuthentication implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return null;
    }

    @Override
    public boolean isAuthenticated() {
      return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

    @Override
    public String getName() {
      return CLIENT_REGISTRATION_ID;
    }
  }
}
