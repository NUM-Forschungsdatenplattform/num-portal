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
