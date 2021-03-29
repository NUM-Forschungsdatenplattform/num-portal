/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.web.feign.config;

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

  private static final String clientRegistrationId = "userStoreClient";

  private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

  @Override
  public OAuth2AccessToken getAccessToken() {

    OAuth2AuthorizedClient auth2AuthorizedClient =
        oAuth2AuthorizedClientManager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
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
      return clientRegistrationId;
    }
  }
}
