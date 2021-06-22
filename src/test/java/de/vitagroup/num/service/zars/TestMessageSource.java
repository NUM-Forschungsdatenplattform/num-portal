package de.vitagroup.num.service.zars;

import java.nio.charset.StandardCharsets;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public class TestMessageSource extends ReloadableResourceBundleMessageSource {

  public TestMessageSource() {
    setBasename("classpath:resourcebundle/resource");
    setUseCodeAsDefaultMessage(true);
    setDefaultEncoding(StandardCharsets.UTF_8.name());
  }
}
