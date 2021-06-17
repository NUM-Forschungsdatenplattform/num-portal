package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Ignore //TODO: Integration testing infrastructure to include keycloak dependency as container
public class AdminControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String ADMIN_PATH = "/admin/user";
  private static final String PROJECT_PATH = "/project";

  private static final String USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a79";

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

    ProjectDto validProject =
        ProjectDto.builder().name("s1").firstHypotheses("fh1").status(ProjectStatus.PENDING).build();
    String studyJson = mapper.writeValueAsString(validProject);

    mockMvc
        .perform(
            post(PROJECT_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(validProject.getName()))
        .andExpect(jsonPath("$.firstHypotheses").value(validProject.getFirstHypotheses()));
  }
}
