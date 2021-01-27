package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.integrationtesting.Roles.RESEARCHER;
import static de.vitagroup.num.integrationtesting.Roles.STUDY_COORDINATOR;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

public class StudyControllerIT extends IntegrationTest {

  @Autowired public MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  private static final String STUDY_PATH = "/study";

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldValidateStudy() {
    StudyDto invalidStudy = StudyDto.builder().build();
    String studyJson = mapper.writeValueAsString(invalidStudy);

    mockMvc
        .perform(
            post(STUDY_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldNotAccessStudyApiWithWrongRole() {
    StudyDto validStudy =
        StudyDto.builder().name("s1").firstHypotheses("fh1").status(StudyStatus.DRAFT).build();
    String studyJson = mapper.writeValueAsString(validStudy);

    mockMvc
        .perform(
            post(STUDY_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_COORDINATOR})
  public void shouldCreateStudy() {
    StudyDto validStudy =
        StudyDto.builder().name("s1").firstHypotheses("fh1").status(StudyStatus.DRAFT).build();
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
