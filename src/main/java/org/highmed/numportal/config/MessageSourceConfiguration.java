package org.highmed.numportal.config;

import org.highmed.numportal.properties.NumProperties;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.annotation.PostConstruct;

@Component
@AllArgsConstructor
public class MessageSourceConfiguration {

  private final NumProperties numProperties;

  @PostConstruct
  private void init() {
    if (StringUtils.isNotEmpty(numProperties.getLocale())) {
      Locale locale = Locale.forLanguageTag(numProperties.getLocale());
      if (locale != null) {
        Locale.setDefault(locale);
      } else {
        Locale.setDefault(Locale.forLanguageTag("en"));
      }
    }
  }

  @Bean(name = "messageSource")
  public MessageSource getMessageSource() {
    ReloadableResourceBundleMessageSource ret = new ReloadableResourceBundleMessageSource();
    ret.setBasename("classpath:resourcebundle/resource");
    ret.setUseCodeAsDefaultMessage(true);
    ret.setDefaultEncoding(StandardCharsets.UTF_8.name());
    return ret;
  }
}
