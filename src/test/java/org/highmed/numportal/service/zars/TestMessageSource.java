package org.highmed.numportal.service.zars;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

public class TestMessageSource extends ReloadableResourceBundleMessageSource {

  public TestMessageSource() {
    setBasename("classpath:resourcebundle/resource");
    setUseCodeAsDefaultMessage(true);
    setDefaultEncoding(StandardCharsets.UTF_8.name());
  }
}
