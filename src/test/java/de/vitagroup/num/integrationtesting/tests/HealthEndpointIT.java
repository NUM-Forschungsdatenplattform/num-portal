package de.vitagroup.num.integrationtesting.tests;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import lombok.SneakyThrows;

public class HealthEndpointIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  private static final String HEALTH_PATH = "/admin/health";

  private static final String LOG_LEVEL_PATH = "/admin/log-level";

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  public void healthEndPointTest() {

            mockMvc
            .perform(
                    get(HEALTH_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  public void logLevelEndPointTest() {

    mockMvc
            .perform(
                    post(LOG_LEVEL_PATH + "/DEBUG")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON));
    mockMvc
            .perform(
                    get(LOG_LEVEL_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.levelStr").value("DEBUG"));
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  public void logLevelModifyEndPointTest() {

    mockMvc
            .perform(
                    post(LOG_LEVEL_PATH + "/OFF")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.levelStr").value("OFF"));
  }

}
