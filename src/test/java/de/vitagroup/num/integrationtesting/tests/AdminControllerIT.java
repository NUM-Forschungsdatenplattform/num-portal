package de.vitagroup.num.integrationtesting.tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import de.vitagroup.num.service.email.EmailService;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static de.vitagroup.num.domain.Roles.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Ignore
public class AdminControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;

  @MockBean
  private EmailService emailService;

  private static final String ADMIN_PATH = "/admin/user";

  private static final String USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a79";

  private static final String USER_ID_TO_BE_APPROVED = "b59e5edb-3121-4e0a-8ccb-af6798207a73";

  @Test
  @WithMockNumUser(
          userId = USER_ID,
          roles = {STUDY_COORDINATOR})
  public void shouldCreateUserOnFirstLoginSuccessfully() throws Exception {
    stubFor(
            WireMock.get("/auth/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a79")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a79\",\"username\": \"new-user\"}")));
    stubFor(WireMock.get("/auth/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a79/role-mappings/realm")
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
            WireMock.get("/auth/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a73")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a73\",\"username\": \"new-user\", \"firstname\":\"John\", \"email\": \"john.doe@vitagroup.ag\"}")));
    mockMvc
            .perform(
                    post(String.format("%s/%s/%s", ADMIN_PATH, USER_ID_TO_BE_APPROVED, "approve"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
  }
}
