package org.highmed.numportal.integrationtesting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.highmed.numportal.integrationtesting.security.WithMockNumUser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.highmed.numportal.domain.model.Roles.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  private static final String ADMIN_PATH = "/admin/user";

  private static final String USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a79";

  private static final String USER_ID_TO_BE_APPROVED = "b59e5edb-3121-4e0a-8ccb-af6798207a73";
  @Autowired
  private ObjectMapper mapper;

  @Test
  @WithMockNumUser(
          userId = USER_ID,
          roles = {STUDY_COORDINATOR})
  public void shouldCreateUserOnFirstLoginSuccessfully() throws Exception {
    stubFor(
            WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a79")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a79\",\"username\": \"new-user\"}")));
    stubFor(WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a79/role-mappings/realm")
            .willReturn(okJson("[{\"id\":\"12345-2f04-4356-8f34-12345\",\"name\":\"STUDY_COORDINATOR\",\"composite\":false,\"clientRole\":false,\"containerId\":\"Num\"}]")));
    mockMvc
            .perform(
                    post(String.format("%s/%s", ADMIN_PATH, USER_ID))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
          roles = {SUPER_ADMIN, ORGANIZATION_ADMIN})
  public void shouldApproveUserSuccessfully() {
    stubFor(
            WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a73")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a73\",\"username\": \"new-user\", \"firstname\":\"John\", \"email\": \"john.doe@highmed.org\"}")));
    mockMvc
            .perform(
                    post(String.format("%s/%s/%s", ADMIN_PATH, USER_ID_TO_BE_APPROVED, "approve"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {SUPER_ADMIN})
  public void shouldUpdateUserStatusSuccessfully() {
    stubFor(
            WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a73")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a73\",\"username\": \"new-user\", \"firstname\":\"John\", \"email\": \"john.doe@highmed.org\", \"enabled\": \"true\"}")));
    stubFor(WireMock.put("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a73").willReturn(okJson("[]")));
    stubFor(WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a73/role-mappings/realm")
            .willReturn(okJson("[{\"id\":\"12345-2f04-1156-8f34-12345\",\"name\":\"RESEARCHER\",\"composite\":false,\"clientRole\":false,\"containerId\":\"Num\"}]")));
    mockMvc
            .perform(
                    post(String.format("%s/%s/%s", ADMIN_PATH, USER_ID_TO_BE_APPROVED, "status"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Boolean.FALSE))
            ).andExpect(status().isOk());
  }

  @Test
  public void shouldGetExternalUrlsSuccessfully() throws Exception {
    mockMvc
            .perform(
                    get("/admin/manuel-url")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userManualUrl.DE").value("user-manual-de"))
            .andExpect(jsonPath("$.userManualUrl.EN").value("user-manual-en"));
  }


}
