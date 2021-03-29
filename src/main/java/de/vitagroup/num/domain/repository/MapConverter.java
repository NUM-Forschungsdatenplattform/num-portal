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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;
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
