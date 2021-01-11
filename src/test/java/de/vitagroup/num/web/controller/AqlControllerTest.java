package de.vitagroup.num.web.controller;

import static de.vitagroup.num.integrationtesting.Roles.ADMIN;
import static de.vitagroup.num.integrationtesting.Roles.RESEARCHER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.integrationtesting.IntegrationTest;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class AqlControllerTest extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private AqlRepository aqlRepository;
  @Autowired private ObjectMapper mapper;

  private static final String userId = "b59e5edb-3121-4e0a-8ccb-af6798207a72";

  @Ignore
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldAccessAqlApi() {
    mockMvc.perform(get("/aql/1")).andExpect(status().isOk());
  }

  @Ignore
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {ADMIN})
  public void shouldNotAccessAqlApi() {
    mockMvc.perform(get("/aql/1")).andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      expiredToken = true)
  public void shouldHandleExpiredToken() {
    mockMvc.perform(get("/aql/1")).andExpect(status().isUnauthorized());
  }

  @SneakyThrows
  @Test
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldReturnNotFound() {
    mockMvc.perform(get("/aql/16577756")).andExpect(status().isNotFound());
  }

  @Ignore
  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldSaveAndRetrieveAqlSuccessfully() {

    Aql aql = Aql.builder().name("t1").description("t2").query("t3").publicAql(true).build();

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(aql);

    mockMvc.perform(post("/admin/user/" + userId)).andExpect(status().isOk());

    mockMvc
        .perform(
            post("/aql").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(requestJson))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
