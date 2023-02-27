package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class AqlControllerIT extends IntegrationTest {

  private static final String AQL_PATH = "/aql";
  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldNotAccessAqlApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", AQL_PATH, 1))).andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      expiredToken = true)
  public void shouldHandleExpiredToken() {
    mockMvc.perform(get(String.format("%s/%s", AQL_PATH, 1))).andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldReturnNotFound() {
    mockMvc
        .perform(get(String.format("%s/%s", AQL_PATH, 16577756)))
        .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      userId = UNAUTHORIZED_USER_ID,
      roles = {RESEARCHER})
  public void shouldHandleNotApprovedUserWhenSavingAql() {

    Aql aql =
        Aql.builder()
            .name("t1")
            .use("use")
            .purpose("purpose")
            .nameTranslated("t1_de")
            .purposeTranslated("purpose_de")
            .useTranslated("use_de")
            .query("t3")
            .publicAql(true)
            .build();
    String aqlJson = mapper.writeValueAsString(aql);

    mockMvc
        .perform(
            post(AQL_PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(aqlJson))
        .andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("For this test to work we need to stub the calls made to keycloak to retrieve users")
  public void shouldSaveAndRetrieveAqlSuccessfully() {

    Aql aql = Aql.builder().name("t1").query("t3").publicAql(true).build();
    String aqlJson = mapper.writeValueAsString(aql);

    MvcResult result =
        mockMvc
            .perform(
                post(AQL_PATH)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aqlJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(aql.getName()))
            .andExpect(jsonPath("$.query").value(aql.getQuery()))
            .andReturn();

    AqlDto dto = mapper.readValue(result.getResponse().getContentAsString(), AqlDto.class);

    mockMvc
        .perform(
            get(String.format("%s/%s", AQL_PATH, dto.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aqlJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(aql.getName()))
        .andExpect(jsonPath("$.query").value(aql.getQuery()));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("For this test to work we need to stub the calls made to keycloak to retrieve users")
  public void shouldSaveAndDeleteAqlSuccessfully() {

    Aql aql = Aql.builder().name("d1").query("d3").publicAql(true).build();
    String aqlJson = mapper.writeValueAsString(aql);

    MvcResult result =
        mockMvc
            .perform(
                post(AQL_PATH)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aqlJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(aql.getName()))
            .andExpect(jsonPath("$.query").value(aql.getQuery()))
            .andReturn();

    AqlDto dto = mapper.readValue(result.getResponse().getContentAsString(), AqlDto.class);

    mockMvc
        .perform(
            get(String.format("%s/%s", AQL_PATH, dto.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aqlJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(aql.getName()))
        .andExpect(jsonPath("$.query").value(aql.getQuery()));

    mockMvc
        .perform(
            delete(String.format("%s/%s", AQL_PATH, dto.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aqlJson))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldValidateAql() {

    Aql noQueryNoDescriptionAql = Aql.builder().name("d1").publicAql(true).build();
    String aqlJson = mapper.writeValueAsString(noQueryNoDescriptionAql);

    mockMvc
        .perform(
            post(AQL_PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(aqlJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {MANAGER})
  public void shouldGetAllCategories() {
    mockMvc.perform(get(AQL_PATH + "/category").with(csrf())).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {MANAGER})
  public void shouldGetAllCategoriesWithPaginationAndSorting() {
    mockMvc.perform(get(AQL_PATH + "/category/all")
            .queryParam("page", "0")
            .queryParam("size", "15")
            .queryParam("sortBy", "name-en")
            .queryParam("sort", "DESC")
            .with(csrf())).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldGetAqlsAsResearcherWithPagination() {
    mockMvc.perform(get(AQL_PATH + "/all")
            .queryParam("page", "0")
            .queryParam("size", "20")
            .queryParam("sortBy", "author")
            .queryParam("sort", "DESC")
            .with(csrf())).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {CRITERIA_EDITOR})
  public void shouldGetAqlsAsCriteriaEditor() {
    mockMvc.perform(get(AQL_PATH).with(csrf())).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldRetrieveParameterValues() {
    MvcResult result =
        mockMvc
            .perform(
                get(AQL_PATH + "/parameter/values")
                    .queryParam(
                        "aqlPath",
                        "/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude")
                    .queryParam("archetypeId", "openEHR-EHR-OBSERVATION.blood_pressure.v2")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_QUANTITY\""));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldRetrieveParameterCodePhrase() {
    MvcResult result =
        mockMvc
            .perform(
                get(AQL_PATH + "/parameter/values")
                    .queryParam(
                        "aqlPath",
                        "/data[at0001]/events[at0002]/data[at0003]/items[at0011]/value/defining_code")
                    .queryParam("archetypeId", "openEHR-EHR-OBSERVATION.pregnancy_status.v0")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_CODED_TEXT\""));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldRetrieveParameterEnumValues() {
    MvcResult result =
        mockMvc
            .perform(
                get(AQL_PATH + "/parameter/values")
                    .queryParam(
                        "aqlPath", "/data[at0001]/events[at0002]/data[at0003]/items[at0007]/value")
                    .queryParam("archetypeId", "openEHR-EHR-OBSERVATION.sofa_score.v0")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_ORDINAL\""));
    assertThat(result.getResponse().getContentAsString(), containsString("\"local::at0028\""));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldRetrieveParameterMagnitude() {
    MvcResult result =
        mockMvc
            .perform(
                get(AQL_PATH + "/parameter/values")
                    .queryParam("aqlPath", "/items[at0005]/value/defining_code")
                    .queryParam("archetypeId", "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_CODED_TEXT\""));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  @Ignore("EhrBase mock is needed to run this test")
  public void shouldRetrieveParameterGender() {
    MvcResult result =
        mockMvc
            .perform(
                get(AQL_PATH + "/parameter/values")
                    .queryParam("aqlPath", "/data[at0002]/items[at0019]/value/defining_code")
                    .queryParam("archetypeId", "openEHR-EHR-EVALUATION.gender.v1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_CODED_TEXT\""));
  }
}
