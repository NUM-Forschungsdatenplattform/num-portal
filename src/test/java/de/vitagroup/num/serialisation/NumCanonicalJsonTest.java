package de.vitagroup.num.serialisation;


import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class NumCanonicalJsonTest {

  private final String CORONA_PATH = "/testdata/corona.json";

  @Test
  void restNumCanonicalJson() throws IOException {
    Composition composition =
      new NumCanonicalJson()
          .unmarshal(
              IOUtils.toString(getClass().getResourceAsStream(CORONA_PATH), StandardCharsets.UTF_8),
              Composition.class
          );
    Assertions.assertNotNull(composition);
  }
}
