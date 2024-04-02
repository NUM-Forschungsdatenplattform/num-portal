package org.highmed.service.email;

import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@AllArgsConstructor
public class MessageSourceWrapper {

  private MessageSource messageSource;

  public String getMessage(String key) {
    return messageSource.getMessage(key, null, Locale.getDefault());
  }

  public String getMessage(String key, Object... vars) {
    return messageSource.getMessage(key, vars, Locale.getDefault());
  }
}
