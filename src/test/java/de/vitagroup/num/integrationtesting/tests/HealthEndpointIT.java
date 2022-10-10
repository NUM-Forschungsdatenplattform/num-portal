package de.vitagroup.num.integrationtesting.tests;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import lombok.SneakyThrows;

public class HealthEndpointIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  private static final String HEALTH_PATH = "/admin/health";


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

}
