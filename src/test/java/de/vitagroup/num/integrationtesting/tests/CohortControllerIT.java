package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.MANAGER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.Operator;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortAqlDto;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Disabled("Should be fixed")
public class CohortControllerIT extends IntegrationTest {

  private static final String COHORT_PATH = "/cohort";
  private static final String COHORT_SIZE_PATH = "/cohort/size";
  private static final String AQL_PATH = "/aql";

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private AqlRepository aqlRepository;
  @Autowired private UserDetailsRepository userDetailsRepository;

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldNotAccessCohortApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", COHORT_PATH, 1))).andExpect(status().isForbidden());
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
            .projectId(1L)
            .cohortGroup(CohortGroupDto.builder().type(Type.AQL).build())
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

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_COORDINATOR, MANAGER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldHandleCohortWithNullParameter() {

    UserDetails userDetails =
        userDetailsRepository.save(UserDetails.builder().userId("user1").approved(true).build());

    Aql aql =
        Aql.builder()
            .id(1L)
            .name("q1")
            .owner(userDetails)
            .query(
                "SELECT\n"
                    + "  o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Gr__e_L_nge__magnitude\n"
                    + "FROM\n"
                    + "  EHR e\n"
                    + "  contains COMPOSITION c1[openEHR-EHR-COMPOSITION.registereintrag.v1]\n"
                    + "  contains OBSERVATION o0[openEHR-EHR-OBSERVATION.height.v2]\n"
                    + "WHERE\n"
                    + "  (c1/archetype_details/template_id/value = 'Körpergröße'\n"
                    + "  and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude = $kg)")
            .publicAql(true)
            .build();
    aqlRepository.save(aql);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("kg", null);

    CohortGroupDto aqlGroupDto =
        CohortGroupDto.builder()
            .type(Type.AQL)
            .operator(Operator.OR)
            .query(
                CohortAqlDto.builder()
                    .id(1L)
                    .query(
                        "SELECT\n"
                            + "  o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Gr__e_L_nge__magnitude\n"
                            + "FROM\n"
                            + "  EHR e\n"
                            + "  contains COMPOSITION c1[openEHR-EHR-COMPOSITION.registereintrag.v1]\n"
                            + "  contains OBSERVATION o0[openEHR-EHR-OBSERVATION.height.v2]\n"
                            + "WHERE\n"
                            + "  (c1/archetype_details/template_id/value = 'Körpergröße'\n"
                            + "  and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude = $kg)")
                    .build())
            .parameters(parameters)
            .build();

    CohortGroupDto cohortGroupDto =
        CohortGroupDto.builder()
            .type(Type.GROUP)
            .operator(Operator.OR)
            .children(List.of(aqlGroupDto))
            .build();

    String cohortDtoJson = mapper.writeValueAsString(cohortGroupDto);

    var response =
        mockMvc
            .perform(
                post(COHORT_SIZE_PATH)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cohortDtoJson))
            .andExpect(status().isOk())
            .andReturn();
  }
}
