package de.vitagroup.num.web.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.UserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("it")
@WebMvcTest(controllers = AdminController.class)
public class AdminControllerTest {

  private static final String HEALTH_PATH = "/admin/health";

  private static final String LOG_LEVEL_PATH = "/admin/log-level";

  @MockBean
  private UserService userService;

  @MockBean
  private UserDetailsService userDetailsService;

  @MockBean
  private HealthEndpoint healthEndpoint;

  private MockMvc mockMvc;

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
  public void healthEndPointTest() {

    HealthComponent healthComponent = Mockito.mock(HealthComponent.class);
    Mockito.when(healthEndpoint.health()).thenReturn(healthComponent);
    Mockito.when(healthComponent.getStatus()).thenReturn(Status.DOWN);

    mockMvc
        .perform(
            get(HEALTH_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

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
