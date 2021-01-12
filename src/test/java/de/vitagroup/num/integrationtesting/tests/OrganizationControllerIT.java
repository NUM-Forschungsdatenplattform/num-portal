package de.vitagroup.num.integrationtesting.tests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class OrganizationControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  public static final String VALID_ORGANIZATION_ID = "12345a";
  public static final String INVALID_ORGANIZATION_ID = "wrong";

  private static final String ORGANIZATION_PATH = "/organization";

  @Test
  @SneakyThrows
  @WithMockNumUser
  public void shouldGetAllOrganizationsSuccessfully() {
    mockMvc.perform(get(ORGANIZATION_PATH)).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser
  public void shouldHandleOrganizationNotFound() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, INVALID_ORGANIZATION_ID)))
        .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser
  public void shouldGetAllOrganizationByIdSuccessfully() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, VALID_ORGANIZATION_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(VALID_ORGANIZATION_ID));
  }
}
