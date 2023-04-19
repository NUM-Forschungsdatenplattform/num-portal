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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Disabled("Should be fixed")
public class TemplateControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String TEMPLATE_PATH = "/template/metadata";

  @Ignore("till is fixed for spring boot 3")
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
