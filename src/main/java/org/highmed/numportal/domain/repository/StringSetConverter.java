package org.highmed.numportal.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@AllArgsConstructor
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

  private final ObjectMapper mapper;

  @Override
  public String convertToDatabaseColumn(Set<String> strings) {

    String stringsJson = null;
    try {
      stringsJson = mapper.writeValueAsString(strings);
    } catch (final JsonProcessingException e) {
      log.error("Cannot convert map to JSON", e);
    }

    return stringsJson;
  }

  @Override
  @NotNull
  public Set<String> convertToEntityAttribute(String stringsJson) {

    if (StringUtils.isEmpty(stringsJson)) {
      return Set.of();
    }

    try {
      return mapper.readValue(stringsJson, Set.class);
    } catch (final IOException e) {
      log.error("Cannot convert JSON to map", e);
    }

    return Set.of();
  }
}
