package de.vitagroup.num.web.feign.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    log.error("Error calling identity provider {}, methodKey {}", response.status(), methodKey);
    switch (response.status()) {
      case 400:
        return new FeignBadRequestException(response.reason());
      case 404:
        return new FeignResourceNotFoundException(response.reason());
      default:
        return new FeignSystemException(response.reason());
    }
  }
}
