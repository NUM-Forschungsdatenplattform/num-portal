package de.vitagroup.num.web.controller;

import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortAqlDto;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.mapper.CohortMapper;
import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.cohort.CohortService;
import de.vitagroup.num.service.cohort.CohortServiceSecurityWrapper;
import de.vitagroup.num.service.exception.ForbiddenException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("it")
@WebMvcTest(controllers = CohortController.class)
@ContextConfiguration(classes = {CohortControllerTest.ControllerTestConfig.class})
public class CohortControllerTest {
  private static final String COHORT_PATH = "/cohort";

  public static final String UNAUTHORIZED_USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a73";

  @Autowired
  private CohortService cohortService;

  @Autowired
  private UserDetailsService userDetailsService;

  @MockBean
  private CohortMapper cohortMapper;

  @Autowired
  private ObjectMapper mapper;

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
  public void shouldNotAccessCohortApiWithWrongRole() {
    mockMvc.perform(
        get(String.format("%s/%s", COHORT_PATH, 1))
            .with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN))
            )
    ).andExpect(status().isForbidden());

    verify(cohortService, never()).getCohort(Mockito.anyLong(), anyString());
  }

  @Test
  @SneakyThrows
  void shouldHandleNotApprovedUserWhenSavingCohort() {

    doThrow(new ForbiddenException(CohortService.class, "Unapproved users should not access the service"))
        .when(userDetailsService).checkIsUserApproved(Mockito.eq(UNAUTHORIZED_USER_ID));

    CohortDto cohortDto =
        CohortDto.builder()
            .name("name")
            .projectId(1L)
            .cohortGroup(
                CohortGroupDto.builder()
                    .type(Type.AQL)
                    .query(CohortAqlDto.builder().id(1L).query("select...").build())
                    .build())
            .build();
    String cohortDtoJson = mapper.writeValueAsString(cohortDto);

    mockMvc
        .perform(
            post(COHORT_PATH)
                .with(jwt()
                    .jwt(builder -> builder.claim("sub", UNAUTHORIZED_USER_ID))
                    .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.STUDY_COORDINATOR)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cohortDtoJson))
        .andExpect(status().isForbidden());

    verify(cohortService, only()).createCohort(any(CohortDto.class), anyString());
    verify(cohortMapper, Mockito.never()).convertToDto(any(Cohort.class));
  }

  @Test
  @SneakyThrows
  public void shouldHandleInvalidCohort() {

    CohortDto cohortDto =
        CohortDto.builder()
            .name("name")
            .projectId(1L)
            .cohortGroup(CohortGroupDto.builder().type(Type.AQL).build())
            .build();
    String cohortDtoJson = mapper.writeValueAsString(cohortDto);

    mockMvc
        .perform(
            post(COHORT_PATH)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + Roles.STUDY_COORDINATOR)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cohortDtoJson))
        .andExpect(status().isBadRequest());

    verify(cohortService, never()).createCohort(any(CohortDto.class), anyString());
  }

  @TestConfiguration
  static class ControllerTestConfig {

    @Bean
    UserDetailsService userDetailsService() {
      return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    CohortService cohortService() {
      CohortService service = new CohortServiceSecurityWrapper(Mockito.mock(CohortService.class), userDetailsService());
      return Mockito.spy(service);
    }
  }
}
