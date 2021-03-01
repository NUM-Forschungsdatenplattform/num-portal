package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
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

  public static final Long VALID_ORGANIZATION_ID = 1L;
  public static final Long INVALID_ORGANIZATION_ID = 12L;

  private static final String ORGANIZATION_PATH = "/organization";

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfully() {
    mockMvc.perform(get(ORGANIZATION_PATH)).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldHandleOrganizationNotFound() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, INVALID_ORGANIZATION_ID)))
        .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationByIdSuccessfully() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, VALID_ORGANIZATION_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(VALID_ORGANIZATION_ID));
  }
}
