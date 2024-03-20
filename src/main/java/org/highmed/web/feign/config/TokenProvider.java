package org.highmed.web.feign.config;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

public interface TokenProvider {

  OAuth2AccessToken getAccessToken();
}
