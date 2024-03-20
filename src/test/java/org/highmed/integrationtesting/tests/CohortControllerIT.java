package org.highmed.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.highmed.integrationtesting.security.WithMockNumUser;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.highmed.domain.dto.CohortAqlDto;
import org.highmed.domain.dto.CohortDto;
import org.highmed.domain.dto.CohortGroupDto;
import org.highmed.domain.model.*;
import org.highmed.domain.model.admin.UserDetails;
import org.highmed.domain.repository.AqlRepository;
import org.highmed.domain.repository.CohortRepository;
import org.highmed.domain.repository.ProjectRepository;
import org.highmed.domain.repository.UserDetailsRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.highmed.domain.model.Roles.*;

public class CohortControllerIT extends IntegrationTest {

  private static final String COHORT_PATH = "/cohort";
  private static final String COHORT_SIZE_PATH = "/cohort/size";

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private AqlRepository aqlRepository;
  @Autowired private UserDetailsRepository userDetailsRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private CohortRepository cohortRepository;


  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldNotAccessCohortApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", COHORT_PATH, 1))).andExpect(status().isForbidden());
  }

  @Test
  @Ignore
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_APPROVER})
  public void shouldAccessCohortApiWithRightRole() {
    UserDetails userOne = UserDetails.builder().userId("b59e5edb-3121-4e0a-8ccb-af6798207a72").approved(true).build();
    String query = "SELECT  c0 as GECCO_Personendaten " +
            " FROM EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] contains CLUSTER c1[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] " +
            " WHERE (c0/archetype_details/template_id/value = 'GECCO_Personendaten' and c1/items[at0001]/value/value > $Geburtsdatum)";
    Aql aql = Aql.builder()
            .name("Geburtsdatum")
            .query(query)
            .owner(userOne)
            .build();
    aql = aqlRepository.save(aql);

    Project approvedProject =
            Project.builder()
                    .name("approved")
                    .goal("Default")
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now())
                    .coordinator(userOne)
                    .researchers(Lists.newArrayList(userOne))
                    .status(ProjectStatus.DRAFT)
                    .build();
    projectRepository.save(approvedProject);
    CohortAql cohortAql = CohortAql.builder()
            .id(aql.getId())
            .name("Geburtsdatum")
            .query(query)
            .build();
    CohortGroup cohortGroup = CohortGroup.builder()
            .type(Type.AQL)
            .operator(Operator.AND)
            .query(cohortAql)
            .parameters(Map.of("Geburtsdatum", "1982-06-08"))
            .build();
    Cohort cohort = Cohort.builder()
            .name("Geburtsdatum cohort")
            .project(projectRepository.findById(1L).get())
            .cohortGroup(cohortGroup)
            .description("just testing")
            .build();
    cohort = cohortRepository.save(cohort);
    Long id = cohort.getId();
    mockMvc.perform(get(String.format("%s/%s", COHORT_PATH, id))).andExpect(status().isOk());
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
  @Ignore
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_COORDINATOR, MANAGER})
  public void shouldHandleCohortWithNullParameter() {

    String query = """
            SELECT
              o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude as Gr__e_L_nge__magnitude
            FROM
              EHR e
              contains COMPOSITION c1[openEHR-EHR-COMPOSITION.registereintrag.v1]
              contains OBSERVATION o0[openEHR-EHR-OBSERVATION.height.v2]
            WHERE
              (c1/archetype_details/template_id/value = 'Körpergröße'
              and o0/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude = $kg)""";
    Aql aql = Aql.builder()
            .name("Body weight")
            .query(query)
            .owner(UserDetails.builder().userId("b59e5edb-3121-4e0a-8ccb-af6798207a72").approved(true).build())
            .build();
    aql = aqlRepository.save(aql);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("kg", null);

    CohortGroupDto aqlGroupDto =
        CohortGroupDto.builder()
            .type(Type.AQL)
            .operator(Operator.OR)
            .query(
                CohortAqlDto.builder()
                    .id(aql.getId())
                    .query(query)
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
    WireMock.stubFor(
            WireMock.post("/ehrbase/rest/openehr/v1/query/aql/")
                    .withRequestBody(WireMock.containing("[openEHR-EHR-OBSERVATION.height.v2] WHERE (c1/archetype_details/template_id/value = 'Körpergröße'"))
                    .willReturn(
                            WireMock.okJson(IOUtils.toString(getClass().getResourceAsStream("/testdata/height_ehr_ids_result.json"),
                                    StandardCharsets.UTF_8))));
    WireMock.stubFor(WireMock.post("/ehrbase/rest/openehr/v1/query/aql/")
            .withRequestBody(WireMock.containing("SELECT e/ehr_id/value FROM ehr e"))
            .willReturn(WireMock.okJson(IOUtils.toString(getClass().getResourceAsStream("/testdata/ehr_id_response.json"),
                    StandardCharsets.UTF_8))));

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
