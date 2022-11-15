package de.vitagroup.num.web.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.dto.TemplateMetadataDto;
import de.vitagroup.num.service.TemplateService;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("it")
@WebMvcTest(controllers = TemplateController.class)
public class TemplateControllerTest {

  private static final String TEMPLATE_PATH = "/template/metadata";
  private MockMvc mockMvc;

  @MockBean
  private TemplateService templateService;

  @Autowired
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders
        .webAppContextSetup(wac)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .alwaysDo(MockMvcResultHandlers.print())
        .build();
  }

  @Test
  @SneakyThrows
  public void shouldGetAllTemplatesSuccessfully() {
    when(templateService.getAllTemplatesMetadata(anyString()))
        .thenReturn(List.of(new TemplateMetadataDto()));

    MvcResult result = mockMvc.perform(get(TEMPLATE_PATH)
        .with(jwt())
    ).andExpect(status().isOk())
        .andReturn();

    List<TemplateMetadataDto> templateMetadataDtos =
        mapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<>() {});

    assertThat(templateMetadataDtos, notNullValue());
    assertThat(templateMetadataDtos.size(), is(1));
  }

}
