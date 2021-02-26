package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class CohortControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String COHORT_PATH = "/cohort";

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldNotAccessCohortApiWithWrongRole() {
    mockMvc
        .perform(get(String.format("%s/%s", COHORT_PATH, 1)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      userId = UNAUTHORIZED_USER_ID,
      roles = {STUDY_COORDINATOR})
  public void shouldHandleNotApprovedUserWhenSavingCohort() {

    CohortDto cohortDto =
        CohortDto.builder()
            .name("name")
            .studyId(1L)
            .cohortGroup(CohortGroupDto.builder().type(Type.PHENOTYPE).phenotypeId(1L).build())
            .build();
    String cohortDtoJson = mapper.writeValueAsString(cohortDto);

    mockMvc
        .perform(
            post(COHORT_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cohortDtoJson))
        .andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_COORDINATOR})
  public void shouldHandleInvalidCohort() {

    CohortDto cohortDto =
        CohortDto.builder()
            .name("name")
            .studyId(1L)
            .cohortGroup(CohortGroupDto.builder().type(Type.PHENOTYPE).build())
            .build();
    String cohortDtoJson = mapper.writeValueAsString(cohortDto);

    mockMvc
        .perform(
            post(COHORT_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cohortDtoJson))
        .andExpect(status().isBadRequest());
  }
}
