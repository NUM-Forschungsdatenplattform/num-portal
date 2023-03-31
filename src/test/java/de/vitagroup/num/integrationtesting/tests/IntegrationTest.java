package de.vitagroup.num.integrationtesting.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.vitagroup.num.NumPortalApplication;
import de.vitagroup.num.TestNumPortalApplication;
import de.vitagroup.num.integrationtesting.config.NumPostgresqlContainer;
import de.vitagroup.num.integrationtesting.security.TokenGenerator;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestNumPortalApplication.class)
@AutoConfigureMockMvc()
@TestPropertySource(locations = "classpath:application.yml")
public abstract class IntegrationTest {

  public static final String UNAUTHORIZED_USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a73";
  private static final String IDENTITY_PROVIDER_URL =
      "/auth/realms/Num/protocol/openid-connect/certs";
  private static final String IDENTITY_PROVIDER_TOKEN_ENDPOINT =
      "/auth/realms/Num/protocol/openid-connect/token";
  private static final String USER_ENDPOINT_USER1 = "/auth/admin/realms/Num/users/user1";
  private static final String USER_ENDPOINT_USER2 = "/auth/admin/realms/Num/users/user2";
  private static final String USER_ENDPOINT_ALL_APPROVERS =
      "/auth/admin/realms/Num/roles/STUDY_APPROVER/users";
  private static final String EHR_BASE_URL = "/ehrbase/rest/openehr/v1/definition/template/adl1.4/";

  @ClassRule
  public static PostgreSQLContainer postgreSQLContainer = NumPostgresqlContainer.getInstance();

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
  }
}
