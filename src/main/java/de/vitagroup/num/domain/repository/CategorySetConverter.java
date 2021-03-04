package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.StudyCategories;
import java.io.IOException;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@AllArgsConstructor
public class CategorySetConverter implements AttributeConverter<Set<StudyCategories>, String> {

  private final ObjectMapper mapper;

  @Override
  public String convertToDatabaseColumn(Set<StudyCategories> categories) {

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
  public Set<StudyCategories> convertToEntityAttribute(String categoriesJson) {

    if (StringUtils.isEmpty(categoriesJson)) {
      return Set.of();
    }

    Set<StudyCategories> categories = null;
    try {
      categories = mapper.readValue(categoriesJson, Set.class);
    } catch (final IOException e) {
      log.error("Cannot convert JSON to map", e);
    }

    return categories;
  }
}
