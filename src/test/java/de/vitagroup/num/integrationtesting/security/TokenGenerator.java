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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

public class TokenGenerator {

  public static String pk;
  public static RSAKey rsaJWK;

  private static final String KEY_ID = "fNIulgSDY0bvx-Qy6iOD-oIROrOKIwsBcaMToZfsBxU";

  static {
    try {
      rsaJWK = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
      pk = "{\n" + "  \"keys\": [" + rsaJWK.toPublicJWK().toJSONString() + "]\n" + "}";
    } catch (JOSEException e) {
      e.printStackTrace();
    }
  }
}
