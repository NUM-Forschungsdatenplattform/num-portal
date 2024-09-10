package org.highmed.numportal.integrationtesting.tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import org.highmed.numportal.TestNumPortalApplication;
import org.highmed.numportal.integrationtesting.config.AttachmentPostgresqlContainer;
import org.highmed.numportal.integrationtesting.config.NumPostgresqlContainer;
import org.highmed.numportal.integrationtesting.security.TokenGenerator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestNumPortalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("itest")
public abstract class IntegrationTest {

  public static final String UNAUTHORIZED_USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a73";
  private static final String IDENTITY_PROVIDER_URL =
      "/realms/Num/protocol/openid-connect/certs";
  private static final String IDENTITY_PROVIDER_TOKEN_ENDPOINT =
      "/realms/Num/protocol/openid-connect/token";
  private static final String USER_ENDPOINT_USER1 = "/admin/realms/Num/users/user1";
  private static final String USER_ENDPOINT_USER2 = "/admin/realms/Num/users/user2";
  private static final String USER_COUNT_ENDPOINT = "/admin/realms/Num/users/count";
  private static final String USER_ENDPOINT_ALL_APPROVERS =
      "/admin/realms/Num/roles/STUDY_APPROVER/users";
  private static final String EHR_BASE_URL = "/ehrbase/rest/openehr/v1/definition/template/adl1.4/";

  @ClassRule
  public static PostgreSQLContainer<NumPostgresqlContainer> postgreSQLContainer = NumPostgresqlContainer.getInstance("numportal");

  @ClassRule
  public static PostgreSQLContainer<AttachmentPostgresqlContainer> attachmentPostgreSQLContainer = AttachmentPostgresqlContainer.getInstance("num-attachment");

  @Autowired public MockMvc mockMvc;
  @Rule public WireMockRule wireMockRule = new WireMockRule(8099);

  @Before
  @SneakyThrows
  public void setup() {
    stubFor(WireMock.get(USER_ENDPOINT_ALL_APPROVERS).willReturn(okJson("[]")));
    stubFor(
        WireMock.get(USER_ENDPOINT_USER1)
            .willReturn(
                okJson(
                    "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User1\"}")));
    stubFor(
        WireMock.get(USER_ENDPOINT_USER2)
            .willReturn(
                okJson(
                    "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User2\"}")));
    stubFor(
        WireMock.post(IDENTITY_PROVIDER_TOKEN_ENDPOINT)
            .willReturn(
                okJson(
                    "{\"token_type\": \"Bearer\",\"access_token\":\"{{randomValue length=20 type='ALPHANUMERIC'}}\"}")));
    stubFor(WireMock.get(IDENTITY_PROVIDER_URL).willReturn(okJson(TokenGenerator.pk)));
    stubFor(
        WireMock.get(EHR_BASE_URL)
            .willReturn(
                okJson(
                    "[{\"template_id\": \"IDCR - Immunisation summary.v0\",\"concept\": \"IDCR - Immunisation summary.v0\",\"archetype_id\": \"openEHR-EHR-COMPOSITION.health_summary.v1\",\"created_timestamp\": \"2020-11-25T16:19:37.812Z\"}]")));
    stubFor(WireMock.get("/admin/realms/Num/roles/SUPER_ADMIN/users").willReturn(okJson("[]")));
    stubFor(
            WireMock.get("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a72")
                    .willReturn(okJson(
                            "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"admin-user\", \"firstname\":\"Admin\", \"email\": \"admin.doe@highmed.org\"}")));
    stubFor(WireMock.get(USER_COUNT_ENDPOINT).willReturn(okJson("2")));
  }
}
