package org.highmed.numportal.integrationtesting.tests;

import lombok.SneakyThrows;
import org.highmed.numportal.TestNumPortalApplication;
import org.highmed.numportal.integrationtesting.config.AttachmentPostgresqlContainer;
import org.highmed.numportal.integrationtesting.config.NumKeycloakMockContainer;
import org.highmed.numportal.integrationtesting.config.NumPostgresqlContainer;
import org.highmed.numportal.integrationtesting.security.TokenGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = TestNumPortalApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("itest")
public abstract class IntegrationTest {

  protected static final Header AUTH_HEADER = new Header("Authorization", "Bearer {{randomValue length=20 type='ALPHANUMERIC'}}");
  public static final String UNAUTHORIZED_USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a73";
  private static final String IDENTITY_PROVIDER_URL =
      "/realms/Num/protocol/openid-connect/certs";
  public static final String IDENTITY_PROVIDER_TOKEN_ENDPOINT =
      "/realms/Num/protocol/openid-connect/token";
  private static final String USER_ENDPOINT_USER1 = "/admin/realms/Num/users/user1";
  private static final String USER_ENDPOINT_USER2 = "/admin/realms/Num/users/user2";
  private static final String USER_ENDPOINT_ALL_APPROVERS =
      "/admin/realms/Num/roles/STUDY_APPROVER/users";
  private static final String EHR_BASE_URL = "/ehrbase/rest/openehr/v1/definition/template/adl1.4/";

  @ClassRule
  public static PostgreSQLContainer<NumPostgresqlContainer> postgreSQLContainer = NumPostgresqlContainer.getInstance("numportal");

  @ClassRule
  public static PostgreSQLContainer<AttachmentPostgresqlContainer> attachmentPostgreSQLContainer = AttachmentPostgresqlContainer.getInstance("num-attachment");

  @ClassRule
  public static NumKeycloakMockContainer keycloakMockContainer = NumKeycloakMockContainer.getInstance();

  @Autowired public MockMvc mockMvc;
  protected static MockServerClient client;

  @Before
  @SneakyThrows
  public void setup() {
    keycloakMockContainer.start();
    client = new MockServerClient("localhost", keycloakMockContainer.getServerPort());

    client
            .when(HttpRequest.request().withMethod("GET").withHeaders(AUTH_HEADER).withPath(USER_ENDPOINT_ALL_APPROVERS))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("[]", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withHeaders(AUTH_HEADER).withPath(USER_ENDPOINT_USER1))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User1\"}", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withHeaders(AUTH_HEADER).withPath(USER_ENDPOINT_USER2))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User2\"}", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("POST").withPath(IDENTITY_PROVIDER_TOKEN_ENDPOINT))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("{\"token_type\": \"Bearer\",\"access_token\":\"{{randomValue length=20 type='ALPHANUMERIC'}}\"}", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withPath(IDENTITY_PROVIDER_URL))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody(TokenGenerator.pk, MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withPath(EHR_BASE_URL))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("[{\"template_id\": \"IDCR - Immunisation summary.v0\",\"concept\": \"IDCR - Immunisation summary.v0\",\"archetype_id\": \"openEHR-EHR-COMPOSITION.health_summary.v1\",\"created_timestamp\": \"2020-11-25T16:19:37.812Z\"}]", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withHeaders(AUTH_HEADER).withPath("/admin/realms/Num/roles/SUPER_ADMIN/users"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("[]", MediaType.JSON_UTF_8));
    client
            .when(HttpRequest.request().withMethod("GET").withHeaders(AUTH_HEADER).withPath("/admin/realms/Num/users/b59e5edb-3121-4e0a-8ccb-af6798207a72"))
            .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"admin-user\", \"firstname\":\"Admin\", \"email\": \"admin.doe@highmed.org\"}", MediaType.JSON_UTF_8));
  }

  @AfterAll
  public void teardown() {
    keycloakMockContainer.stop();
  }
}
