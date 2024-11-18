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
        feature.search-by-manager = false
        """)
public class ManagerControllerFeatureDisabledIT extends IntegrationTest {

  private static final String MANAGER_PATH = "/manager";

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"MANAGER"})
  public void executeQuery() {
    QueryDto queryDto = new QueryDto();

    mockMvc.perform(post(MANAGER_PATH + "/execute/query").with(csrf())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(mapper.writeValueAsString(queryDto))
    ).andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser()
  public void executeQueryAsNonAuthorizedUser() {
    QueryDto queryDto = new QueryDto();

    mockMvc.perform(post(MANAGER_PATH + "/execute/query").with(csrf())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(mapper.writeValueAsString(queryDto))
    ).andExpect(status().isNotFound());
  }
}