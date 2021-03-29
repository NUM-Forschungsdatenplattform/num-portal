/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
