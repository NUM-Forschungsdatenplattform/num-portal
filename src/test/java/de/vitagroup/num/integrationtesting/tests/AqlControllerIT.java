package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.integrationtesting.Roles.ADMIN;
import static de.vitagroup.num.integrationtesting.Roles.RESEARCHER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class AqlControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String AQL_PATH = "/aql";

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {ADMIN})
  public void shouldNotAccessAqlApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", AQL_PATH, 1))).andExpect(status().isUnauthorized());
  }

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

    Aql aql = Aql.builder().name("t1").description("t2").query("t3").publicAql(true).build();
    String aqlJson = mapper.writeValueAsString(aql);

    mockMvc
        .perform(
            post(AQL_PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(aqlJson))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldSaveAndRetrieveAqlSuccessfully() {

    Aql aql = Aql.builder().name("t1").description("t2").query("t3").publicAql(true).build();
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
            .andExpect(jsonPath("$.description").value(aql.getDescription()))
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
        .andExpect(jsonPath("$.description").value(aql.getDescription()));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldSaveAndDeleteAqlSuccessfully() {

    Aql aql = Aql.builder().name("d1").description("d2").query("d3").publicAql(true).build();
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
            .andExpect(jsonPath("$.description").value(aql.getDescription()))
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
        .andExpect(jsonPath("$.description").value(aql.getDescription()));

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

    Aql noQueryNoDescriptionAql = Aql.builder().name("d1").publicAql(true).build();
    String aqlJson = mapper.writeValueAsString(noQueryNoDescriptionAql);

    mockMvc
        .perform(
            post(AQL_PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(aqlJson))
        .andExpect(status().isBadRequest());
  }
}
