package org.highmed.numportal.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.highmed.numportal.domain.dto.AqlCategoryDto;
import org.highmed.numportal.domain.dto.AqlDto;
import org.highmed.numportal.domain.dto.ParameterOptionsDto;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.highmed.numportal.domain.model.Roles.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
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

  @Ignore("fix mock expired token")
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

    AqlDto aql =
        AqlDto.builder()
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
  @WithMockNumUser(roles = {CRITERIA_EDITOR})
  public void shouldSaveAndRetrieveAqlSuccessfully() {

    AqlDto aql = AqlDto.builder()
            .name("t1")
            .use("aql use")
            .purpose("aql purpose")
            .nameTranslated("aql name translated")
            .useTranslated("aql use translated")
            .purposeTranslated("aql purpose translated")
            .query("t3")
            .publicAql(true).build();
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
            .andExpect(jsonPath("$.purpose").value(aql.getPurpose()))
            .andExpect(jsonPath("$.purposeTranslated").value(aql.getPurposeTranslated()))
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
        .andExpect(jsonPath("$.query").value(aql.getQuery()))
        .andExpect(jsonPath("$.use").value(aql.getUse()))
        .andExpect(jsonPath("$.useTranslated").value(aql.getUseTranslated()));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {CRITERIA_EDITOR})
  public void shouldSaveAndDeleteAqlSuccessfully() {

    AqlDto aql = AqlDto.builder()
            .name("d1")
            .nameTranslated("d1 translated")
            .query("d3")
            .use("d1 aql use")
            .purpose("d1 aql purpose")
            .purposeTranslated("d1 aql purpose translated")
            .useTranslated("d1 aql use translated")
            .publicAql(true).build();
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

    AqlDto noQueryNoDescriptionAql = AqlDto.builder()
            .name("d1")
            .publicAql(true).build();
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
  @Ignore
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldRetrieveParameterValues() {
    client
            .when(HttpRequest.request().withMethod("POST").withHeaders(AUTH_HEADER).withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody("openEHR-EHR-OBSERVATION.blood_pressure.v2"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/testdata/blood_pressure_response.json")), StandardCharsets.UTF_8), org.mockserver.model.MediaType.JSON_UTF_8));
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

    assertThat(result.getResponse().getStatus(), is(HttpStatus.OK.value()));
    assertThat(
        result.getResponse().getContentAsString(), containsString("\"type\":\"DV_QUANTITY\""));

    client
            .when(HttpRequest.request().withMethod("POST").withHeaders(AUTH_HEADER).withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody("openEHR-EHR-OBSERVATION.pregnancy_status.v0"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/testdata/pregnancy_status_response.json")), StandardCharsets.UTF_8), org.mockserver.model.MediaType.JSON_UTF_8));
    MvcResult result2 =
            mockMvc
                    .perform(
                            get(AQL_PATH + "/parameter/values")
                                    .queryParam(
                                            "aqlPath",
                                            "/data[at0001]/events[at0002]/data[at0003]/items[at0011]/value/defining_code/code_string")
                                    .queryParam("archetypeId", "openEHR-EHR-OBSERVATION.pregnancy_status.v0")
                                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
    assertThat(
            result2.getResponse().getContentAsString(), containsString("\"type\":\"DV_CODED_TEXT\""));

    client
            .when(HttpRequest.request().withMethod("POST").withHeaders(AUTH_HEADER).withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody("openEHR-EHR-OBSERVATION.clinical_frailty_scale.v1"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/testdata/frailty_score_response.json")), StandardCharsets.UTF_8), org.mockserver.model.MediaType.JSON_UTF_8));
    MvcResult result3 =
            mockMvc
                    .perform(
                            get(AQL_PATH + "/parameter/values")
                                    .queryParam(
                                            "aqlPath", "/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value")
                                    .queryParam("archetypeId", "openEHR-EHR-OBSERVATION.clinical_frailty_scale.v1")
                                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
    assertThat(
            result3.getResponse().getContentAsString(), containsString("\"type\":\"DV_ORDINAL\""));

    client
            .when(HttpRequest.request().withMethod("POST").withHeaders(AUTH_HEADER).withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody("openEHR-EHR-CLUSTER.laboratory_test_analyte.v1"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/testdata/laboratory_antithrombin_result.json")), StandardCharsets.UTF_8), org.mockserver.model.MediaType.JSON_UTF_8));
    MvcResult result4 =
            mockMvc
                    .perform(
                            get(AQL_PATH + "/parameter/values")
                                    .queryParam("aqlPath", "/items[at0001]/value/magnitude")
                                    .queryParam("archetypeId", "openEHR-EHR-CLUSTER.laboratory_test_analyte.v1")
                                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
    assertThat(
            result4.getResponse().getContentAsString(), containsString("\"type\":\"DV_QUANTITY\""));
    assertThat(
            result4.getResponse().getContentAsString(), containsString("\"unit\":\"mg/dL\""));

    client
            .when(HttpRequest.request().withMethod("POST").withHeaders(AUTH_HEADER).withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody("openEHR-EHR-EVALUATION.gender.v1"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/testdata/gender_response.json")), StandardCharsets.UTF_8), org.mockserver.model.MediaType.JSON_UTF_8));
    MvcResult genderResult =
            mockMvc
                    .perform(
                            get(AQL_PATH + "/parameter/values")
                                    .queryParam("aqlPath", "/data[at0002]/items[at0019]/value/defining_code/code_string")
                                    .queryParam("archetypeId", "openEHR-EHR-EVALUATION.gender.v1")
                                    .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("DV_CODED_TEXT"))
                    .andReturn();
    ParameterOptionsDto dto = mapper.readValue(genderResult.getResponse().getContentAsString(), ParameterOptionsDto.class);
    assertThat((String)dto.getOptions().get("female"),containsString("Female"));
    assertThat((String)dto.getOptions().get("male"),containsString("Male"));
  }

  @Test
  @SneakyThrows
  @Ignore
  @WithMockNumUser(roles = {CRITERIA_EDITOR})
  public void shouldSaveAndDeleteAqlCategorySuccessfully() {

    Map<String, String> name = new HashMap<>();
    name.put("en", "aql category name en");
    name.put("de", "aql category name test de");
    AqlCategoryDto aqlCategoryDto = AqlCategoryDto.builder()
            .name(name)
            .build();
    String aqlCatJson = mapper.writeValueAsString(aqlCategoryDto);

    MvcResult result =
            mockMvc
                    .perform(
                            post(AQL_PATH + "/category")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(aqlCatJson))
                    .andExpect(status().isOk())
                    .andReturn();

    AqlCategoryDto dto = mapper.readValue(result.getResponse().getContentAsString(), AqlCategoryDto.class);

    mockMvc
            .perform(
                    delete(String.format("%s/%s", AQL_PATH + "/category", dto.getId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
  }

}
