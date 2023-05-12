package de.vitagroup.num.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import de.vitagroup.num.domain.*;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CohortAqlDto;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import de.vitagroup.num.service.CohortService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.vitagroup.num.domain.Roles.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
  @Autowired
  private CohortService cohortService;

  @Before
  public void setupCohortData() {
    String query = "SELECT  c0 as GECCO_Personendaten " +
            " FROM EHR e contains COMPOSITION c0[openEHR-EHR-COMPOSITION.registereintrag.v1] contains CLUSTER c1[openEHR-EHR-CLUSTER.person_birth_data_iso.v0] " +
            " WHERE (c0/archetype_details/template_id/value = 'GECCO_Personendaten' and c1/items[at0001]/value/value > $Geburtsdatum)";
    CohortAqlDto cohortAqlDto = CohortAqlDto.builder()
            .id(1L)
            .name("Geburtsdatum")
            .query(query)
            .build();

    CohortGroupDto first =
            CohortGroupDto.builder()
                    .type(Type.AQL)
                    .query(cohortAqlDto)
                    .parameters(Map.of("Geburtsdatum", "1982-06-08"))
                    .operator(Operator.AND)
                    .build();
    CohortGroupDto andCohort =
            CohortGroupDto.builder()
                    .type(Type.GROUP)
                    .operator(Operator.AND)
                    .children(List.of(first))
                    .build();
    CohortDto cohortDto =
            CohortDto.builder()
                    .name("Cohort for birthdate")
                    .projectId(1L)
                    .cohortGroup(andCohort)
                    .build();
    UserDetails userOne = UserDetails.builder().userId("b59e5edb-3121-4e0a-8ccb-af6798207a72").approved(true).build();
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
    Map<String, String> tr1 = new HashMap<>();
    tr1.put("en", "category one in english");
    tr1.put("de", "category one in german");
    Aql aqlOne = Aql.builder()
            .name("Geburtsdatum")
            .publicAql(true)
            .query(query)
            .owner(UserDetails.builder()
                    .userId("b59e5edb-3121-4e0a-8ccb-af6798207a72")
                    .build())
            .build();
    aqlRepository.save(aqlOne);
    projectRepository.save(approvedProject);
    cohortService.createCohort(cohortDto, "b59e5edb-3121-4e0a-8ccb-af6798207a72");
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldNotAccessCohortApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", COHORT_PATH, 1))).andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_APPROVER})
  public void shouldAccessCohortApiWithRightRole() {
    mockMvc.perform(get(String.format("%s/%s", COHORT_PATH, 1))).andExpect(status().isOk());
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
    WireMock.stubFor(
            WireMock.post("/ehrbase/rest/openehr/v1/query/aql/")
                    .withRequestBody(WireMock.containing("[openEHR-EHR-OBSERVATION.height.v2] where (c1/archetype_details/template_id/value = 'Körpergröße'"))
                    .willReturn(
                            WireMock.okJson(IOUtils.toString(getClass().getResourceAsStream("/testdata/height_ehr_ids_result.json"),
                                    StandardCharsets.UTF_8))));
    WireMock.stubFor(WireMock.post("/ehrbase/rest/openehr/v1/query/aql/")
            .withRequestBody(WireMock.containing("Select e/ehr_id/value as F1 from EHR e"))
            .willReturn(WireMock.okJson(IOUtils.toString(getClass().getResourceAsStream("/testdata/ehr_id_response.json"),
                    StandardCharsets.UTF_8))));

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
