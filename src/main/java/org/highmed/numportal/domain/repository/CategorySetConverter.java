package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.ProjectCategories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Set;

@Slf4j
@AllArgsConstructor
public class CategorySetConverter implements AttributeConverter<Set<ProjectCategories>, String> {

  private final ObjectMapper mapper;

  @Override
  public String convertToDatabaseColumn(Set<ProjectCategories> categories) {

    String categoriesJson = null;
    try {
      categoriesJson = mapper.writeValueAsString(categories);
    } catch (final JsonProcessingException e) {
      log.error("Cannot convert map to JSON", e);
    }

    return categoriesJson;
  }

  @Override
  @NotNull
  public Set<ProjectCategories> convertToEntityAttribute(String categoriesJson) {

    if (StringUtils.isEmpty(categoriesJson)) {
      return Set.of();
    }

    try {
      return mapper.readValue(categoriesJson, Set.class);
    } catch (final IOException e) {
      log.error("Cannot convert JSON to map", e);
    }

    return Set.of();
  }
}
