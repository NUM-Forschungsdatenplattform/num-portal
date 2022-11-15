package de.vitagroup.num.web.controller;

import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.OrganizationService;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("it")
@WebMvcTest(controllers = OrganizationController.class)
@ContextConfiguration(classes = {OrganizationControllerTest.OrganizationControllerTestConfiguration.class})
public class OrganizationControllerTest {

  public static final Long VALID_ORGANIZATION_ID = 1L;
  public static final Long INVALID_ORGANIZATION_ID = 12L;

  private static final String ORGANIZATION_PATH = "/organization";

  private MockMvc mockMvc;

  @MockBean
  private OrganizationService organizationService;

  @Autowired
  private OrganizationMapper organizationMapper;

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
  public void shouldGetAllOrganizationsSuccessfully() {
    mockMvc.perform(
        get(ORGANIZATION_PATH)
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN))
            )
        )
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void shouldHandleOrganizationNotFound() {

    ResourceNotFound result = new ResourceNotFound(
        OrganizationService.class,
        "Organization not found for id : " + INVALID_ORGANIZATION_ID
    );
    Mockito.doThrow(result)
        .when(organizationService).getOrganizationById(Mockito.eq(INVALID_ORGANIZATION_ID));

    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, INVALID_ORGANIZATION_ID))
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN)))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  public void shouldGetAllOrganizationByIdSuccessfully() {

    Organization organization = new Organization();
    organization.setId(VALID_ORGANIZATION_ID);
    when(organizationService.getOrganizationById(Mockito.eq(VALID_ORGANIZATION_ID)))
        .thenReturn(organization);

    mockMvc
        .perform(get(String.format("%s/%s", ORGANIZATION_PATH, VALID_ORGANIZATION_ID))
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN)))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(VALID_ORGANIZATION_ID));
  }

  @Test
  @SneakyThrows
  public void shouldGetAllOrganizationsSuccessfullyWithPagination() {
    when(organizationService.getAllOrganizations(anyList(), anyString(), any(), any(
        Pageable.class))).thenReturn(new PageImpl<>(emptyList()));

    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
            .queryParam("page", "0")
            .queryParam("size", "5")
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN)))
        )
        .andExpect(status().isOk());

    verify(organizationService, only()).getAllOrganizations(anyList(), anyString(), any(), any(
        Pageable.class));
  }

  @Test
  @SneakyThrows
  public void shouldGetAllOrganizationsSuccessfullyWithPaginationAndFilter() {

    when(organizationService.getAllOrganizations(anyList(), anyString(), any(), any(
        Pageable.class))).thenReturn(new PageImpl<>(emptyList()));

    mockMvc.perform(get(ORGANIZATION_PATH + "/all")
            .queryParam("page", "0")
            .queryParam("filter[name]", "dummySearchInput")
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN)))
        )
        .andExpect(status().isOk());
  }

  @TestConfiguration
  static class OrganizationControllerTestConfiguration {
    @Bean
    OrganizationMapper organizationMapper() {
      return new OrganizationMapper(new ModelMapper());
    }
  }
}
