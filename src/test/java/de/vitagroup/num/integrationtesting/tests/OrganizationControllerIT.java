package de.vitagroup.num.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.OrganizationRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.Optional;

import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrganizationControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  public static final Long VALID_ORGANIZATION_ID = 1L;
  public static final Long INVALID_ORGANIZATION_ID = 12L;

  private static final String ORGANIZATION_PATH = "/organization";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private UserDetailsRepository userDetailsRepository;

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

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfullyWithPagination() {
    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
                    .queryParam("page", "0")
                    .queryParam("size", "5"))
            .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldGetAllOrganizationsSuccessfullyWithPaginationAndFilter() {
    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
                    .queryParam("page", "0")
                    .queryParam("filter[search]", "dummySearchInput"))
            .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldSaveAndDeleteOrganizationSuccessfully() {
    OrganizationDto request = OrganizationDto.builder()
            .name("organization Test")
            .mailDomains(Collections.emptySet())
            .build();
    String organizationJson = mapper.writeValueAsString(request);

    MvcResult result =
            mockMvc
                    .perform(
                            post(ORGANIZATION_PATH)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(organizationJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(request.getName()))
                    .andReturn();

    OrganizationDto dto = mapper.readValue(result.getResponse().getContentAsString(), OrganizationDto.class);

    mockMvc
            .perform(
                    delete(String.format("%s/%s", ORGANIZATION_PATH, dto.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldHandleDeleteOrganizationWithUsers() {
    OrganizationDto request = OrganizationDto.builder()
            .name("Organization TestDelete")
            .mailDomains(Collections.emptySet())
            .build();
    String organizationJson = mapper.writeValueAsString(request);

    mockMvc
            .perform(
                    post(ORGANIZATION_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(organizationJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(request.getName()))
            .andReturn();

    Optional<Organization> organization = organizationRepository.findByName("Organization TestDelete");
    UserDetails userDetails = UserDetails.builder().userId("userOne").approved(true).organization(organization.get()).build();
    userDetailsRepository.save(userDetails);

    mockMvc
            .perform(
                    delete(String.format("%s/%s", ORGANIZATION_PATH, organization.get().getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }
}
