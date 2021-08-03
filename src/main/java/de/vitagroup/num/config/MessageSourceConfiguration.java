package de.vitagroup.num.config;

import de.vitagroup.num.properties.NumProperties;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

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
