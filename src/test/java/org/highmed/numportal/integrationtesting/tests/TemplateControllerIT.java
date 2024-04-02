package org.highmed.numportal.integrationtesting.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.highmed.numportal.domain.dto.TemplateMetadataDto;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                new TypeReference<>() {
                });

    assertThat(templateMetadataDtos, notNullValue());
    assertThat(templateMetadataDtos.size(), is(1));
  }
}
