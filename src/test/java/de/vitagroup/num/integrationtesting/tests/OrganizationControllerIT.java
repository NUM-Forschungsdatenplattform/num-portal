package de.vitagroup.num.integrationtesting.tests;

import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("Should be fixed")
public class OrganizationControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  public static final Long VALID_ORGANIZATION_ID = 1L;
  public static final Long INVALID_ORGANIZATION_ID = 12L;

  private static final String ORGANIZATION_PATH = "/organization";

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfully() {
    mockMvc.perform(get(ORGANIZATION_PATH)).andExpect(status().isOk());
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldHandleOrganizationNotFound() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, INVALID_ORGANIZATION_ID)))
        .andExpect(status().isNotFound());
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationByIdSuccessfully() {
    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, VALID_ORGANIZATION_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(VALID_ORGANIZATION_ID));
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfullyWithPagination() {
    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
                    .queryParam("page", "0")
                    .queryParam("size", "5"))
            .andExpect(status().isOk());
  }

  @Ignore(
          "till is fixed for spring boot 3")
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfullyWithPaginationAndFilter() {
    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
                    .queryParam("page", "0")
                    .queryParam("filter[name]", "dummySearchInput"))
            .andExpect(status().isOk());
  }
}
