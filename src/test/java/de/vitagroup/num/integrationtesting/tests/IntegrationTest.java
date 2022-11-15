package de.vitagroup.num.integrationtesting.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import de.vitagroup.num.NumPortalApplication;
import de.vitagroup.num.WireMockInitializer;
import de.vitagroup.num.integrationtesting.config.NumPostgresqlContainer;
import de.vitagroup.num.integrationtesting.security.TokenGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Slf4j
@ActiveProfiles("it")
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = NumPortalApplication.class)
@ContextConfiguration(initializers = WireMockInitializer.class)
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


  @Container
  public static PostgreSQLContainer postgreSQLContainer = NumPostgresqlContainer.getInstance();


  @RegisterExtension
  static WireMockExtension wireMockServer = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("keycloak.url", wireMockServer::baseUrl);
  }

  MockMvc mockMvc;

  @BeforeEach
  @SneakyThrows
  public void setup(WebApplicationContext wac, WireMockRuntimeInfo wmRuntimeInfo) {
    Assertions.assertNotNull(wmRuntimeInfo);

    this.mockMvc = MockMvcBuilders
        .webAppContextSetup(wac)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .alwaysDo(MockMvcResultHandlers.print())
        .build();

    WireMock wireMock = wmRuntimeInfo.getWireMock();
    log.info("WIRE_MOCK -- BASE_URL : {} ", wmRuntimeInfo.getHttpBaseUrl());

    wireMock.register(WireMock.get(USER_ENDPOINT_ALL_APPROVERS).willReturn(okJson("[]")));
    wireMock.register(
        WireMock.get(USER_ENDPOINT_USER1)
            .willReturn(
                okJson(
                    "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User1\"}")));
    wireMock.register(
        WireMock.get(USER_ENDPOINT_USER2)
            .willReturn(
                okJson(
                    "{\"id\": \"b59e5edb-3121-4e0a-8ccb-af6798207a72\",\"username\": \"User2\"}")));
    wireMock.register(
        WireMock.post(IDENTITY_PROVIDER_TOKEN_ENDPOINT)
            .willReturn(
                okJson(
                    "{\"token_type\": \"Bearer\",\"access_token\":\"{{randomValue length=20 type='ALPHANUMERIC'}}\"}")));
    wireMock.register(WireMock.get(IDENTITY_PROVIDER_URL).willReturn(okJson(TokenGenerator.pk)));
    wireMock.register(
        WireMock.get(EHR_BASE_URL)
            .willReturn(
                okJson(
                    "[{\"template_id\": \"IDCR - Immunisation summary.v0\",\"concept\": \"IDCR - Immunisation summary.v0\",\"archetype_id\": \"openEHR-EHR-COMPOSITION.health_summary.v1\",\"created_timestamp\": \"2020-11-25T16:19:37.812Z\"}]")));

  }
}
