package org.highmed.numportal.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.highmed.numportal.domain.dto.QueryDto;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = """
        feature.search-with-aql = false
        """)
public class QueryControllerFeatureDisabledIT extends IntegrationTest {

  @Autowired
  public MockMvc mockMvc;

  private static final String PATH = "/query/execute";
  @Autowired
  private ObjectMapper mapper;

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"MANAGER"})
  public void execute() {
    QueryDto queryDto = new QueryDto();

    mockMvc.perform(post(PATH).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(queryDto))
    ).andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser()
  public void executeAsNonAuthorizedUser() {
    QueryDto queryDto = new QueryDto();

    mockMvc.perform(post(PATH).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(queryDto))
    ).andExpect(status().isNotFound());
  }
}
