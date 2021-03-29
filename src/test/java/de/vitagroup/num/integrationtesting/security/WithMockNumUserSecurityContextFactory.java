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

package de.vitagroup.num.integrationtesting.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import java.time.Instant;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockNumUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockNumUser> {

  @SneakyThrows
  @Override
  public SecurityContext createSecurityContext(WithMockNumUser numUser) {

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    JWSSigner signer = new RSASSASigner(TokenGenerator.rsaJWK);

    JWSObject jwsObject =
        new JWSObject(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(TokenGenerator.rsaJWK.getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            new Payload(createPayload(numUser)));

    jwsObject.sign(signer);

    String token = jwsObject.serialize();

    context.setAuthentication(new BearerTokenAuthenticationToken(token));

    return context;
  }

  private String createPayload(WithMockNumUser numUser) {

    JSONObject payload = new JSONObject();

    payload.put(
        "exp",
        numUser.expiredToken()
            ? Instant.now().toEpochMilli()
            : Instant.now().plusSeconds(60L).toEpochMilli());

    payload.put("iat", Instant.now().toEpochMilli());
    payload.put("username", numUser.username());
    payload.put("name", numUser.name());
    payload.put("email", numUser.email());
    payload.put("sub", numUser.userId());

    JSONObject roles = new JSONObject();
    roles.put("roles", new JSONArray(numUser.roles()));

    payload.put("realm_access", roles);

    return payload.toString();
  }
}
