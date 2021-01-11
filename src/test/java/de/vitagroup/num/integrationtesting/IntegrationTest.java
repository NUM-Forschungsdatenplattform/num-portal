package de.vitagroup.num.integrationtesting;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.vitagroup.num.NumPortalApplication;
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
    classes = NumPortalApplication.class)
@AutoConfigureMockMvc()
@TestPropertySource(locations = "classpath:application.yml")
public abstract class IntegrationTest {

  @Autowired public MockMvc mockMvc;

  @Rule public WireMockRule wireMockRule = new WireMockRule();

  @ClassRule
  public static PostgreSQLContainer postgreSQLContainer = NumPostgresqlContainer.getInstance();

  @Before
  public void setup() {
    stubFor(
        WireMock.get("/auth/realms/Num/protocol/openid-connect/certs")
            .willReturn(okJson(TokenGenerator.pk)));
  }

  @SneakyThrows
  public void createUser(String userId){
    mockMvc.perform(get("/admin/user/" + userId)).andExpect(status().isOk());
  }

}
