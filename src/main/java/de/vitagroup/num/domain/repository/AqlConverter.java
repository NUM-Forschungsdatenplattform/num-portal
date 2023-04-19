package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.CohortAql;
import jakarta.persistence.AttributeConverter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@AllArgsConstructor
public class AqlConverter implements AttributeConverter<CohortAql, String> {

  private ObjectMapper mapper;

  @Override
  public String convertToDatabaseColumn(CohortAql expression) {
    try {
      return mapper.writeValueAsString(expression);
    } catch (JsonProcessingException e) {
      log.error("Cannot convert expression to JSON", e);
    }

    return StringUtils.EMPTY;
  }

  @Override
  public CohortAql convertToEntityAttribute(String json) {
    try {
      return mapper.readValue(json, CohortAql.class);
    } catch (JsonProcessingException e) {
      log.error("Cannot convert JSON to expression", e);
    }

    return null;
  }
}
