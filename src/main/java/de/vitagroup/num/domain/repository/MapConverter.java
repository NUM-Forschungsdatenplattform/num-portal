package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class MapConverter implements AttributeConverter<Map<String, Object>, String> {

  private ObjectMapper mapper;

  @Override
  public String convertToDatabaseColumn(Map<String, Object> parameters) {

    String parametersJson = null;
    try {
      parametersJson = mapper.writeValueAsString(parameters);
    } catch (final JsonProcessingException e) {
      log.error("Cannot convert map to JSON", e);
    }

    return parametersJson;
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String parametersJson) {

    if (StringUtils.isEmpty(parametersJson)) {
      return null;
    }

    Map<String, Object> parameters = null;
    try {
      parameters = mapper.readValue(parametersJson, Map.class);
    } catch (final IOException e) {
      log.error("Cannot convert JSON to map", e);
    }

    return parameters;
  }
}
