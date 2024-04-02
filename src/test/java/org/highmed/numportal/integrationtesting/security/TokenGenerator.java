package org.highmed.numportal.integrationtesting.security;

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
