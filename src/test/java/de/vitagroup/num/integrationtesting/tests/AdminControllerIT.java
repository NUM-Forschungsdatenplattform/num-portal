package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class AdminControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String ADMIN_PATH = "/admin/user";
  private static final String STUDY_PATH = "/study";

  private static final String USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a79";

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldNotAccessAqlApiWithWrongRole() {
    mockMvc
        .perform(get(String.format("%s/%s", ADMIN_PATH, "someuser")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      userId = USER_ID,
      roles = {SUPER_ADMIN, STUDY_COORDINATOR})
  public void shouldCreateAndApproveUserSuccessfully() {

    mockMvc
        .perform(
            post(String.format("%s/%s", ADMIN_PATH, USER_ID))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post(String.format("%s/%s/%s", ADMIN_PATH, USER_ID, "approve"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    StudyDto validStudy =
        StudyDto.builder().name("s1").firstHypotheses("fh1").status(StudyStatus.PENDING).build();
    String studyJson = mapper.writeValueAsString(validStudy);

    mockMvc
        .perform(
            post(STUDY_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(validStudy.getName()))
        .andExpect(jsonPath("$.firstHypotheses").value(validStudy.getFirstHypotheses()));
  }
}
