package org.highmed.numportal.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.ehrbase.openehr.sdk.response.dto.QueryResponseData;

import org.highmed.numportal.TestNumPortalApplication;
import org.highmed.numportal.domain.dto.QueryDto;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.StringBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestNumPortalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("itest")
@TestPropertySource(properties = """
        feature.search-by-manager = true
        """)
public class ManagerControllerIT extends IntegrationTest {

  private static final String MANAGER_PATH = "/manager";

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {"MANAGER"})
  public void executeQuery() {
    String query = "SELECT *";
    QueryDto queryDto = new QueryDto();
    queryDto.setAql(query);
    QueryResponseData queryResponseData = new QueryResponseData();
    var expectedResult = mapper.writeValueAsString(queryResponseData);

    ehrClient
        .when(HttpRequest.request().withMethod("POST").withPath("/ehrbase/rest/openehr/v1/query/aql/").withBody(StringBody.subString(query, StandardCharsets.UTF_8)))
        .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(expectedResult, org.mockserver.model.MediaType.JSON_UTF_8));

    MvcResult result =
        mockMvc
            .perform(
                post(MANAGER_PATH + "/execute/query").with(csrf())
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(mapper.writeValueAsString(queryDto)))
            .andExpect(status().isOk())
            .andReturn();

    assertThat(result.getResponse().getContentAsString(), equalTo(expectedResult));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser()
  public void executeQueryAsNonAuthorizedUser() {
    var query = "SELECT *";
    QueryDto queryDto = new QueryDto();
    queryDto.setAql(query);

    mockMvc.perform(post(MANAGER_PATH + "/execute/query").with(csrf())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(mapper.writeValueAsString(queryDto))
    ).andExpect(status().isForbidden());
  }
}