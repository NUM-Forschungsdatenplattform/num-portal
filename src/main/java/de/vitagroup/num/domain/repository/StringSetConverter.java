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
import java.io.IOException;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;
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

    Set<String> strings = null;
    try {
      strings = mapper.readValue(stringsJson, Set.class);
    } catch (final IOException e) {
      log.error("Cannot convert JSON to map", e);
    }

    return strings;
  }
}
