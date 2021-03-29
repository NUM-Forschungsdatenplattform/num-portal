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

package de.vitagroup.num.integrationtesting.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class TemplateControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String TEMPLATE_PATH = "/template/metadata";

  @Test
  @SneakyThrows
  @WithMockNumUser
  public void shouldGetAllTemplatesSuccessfully() {
    MvcResult result = mockMvc.perform(get(TEMPLATE_PATH)).andExpect(status().isOk()).andReturn();

    List<TemplateMetadataDto> templateMetadataDtos =
        mapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<TemplateMetadataDto>>() {});

    assertThat(templateMetadataDtos, notNullValue());
    assertThat(templateMetadataDtos.size(), is(1));
  }
}
