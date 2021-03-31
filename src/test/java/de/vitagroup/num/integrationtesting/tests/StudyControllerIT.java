package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.ORGANIZATION_ADMIN;
import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import java.time.LocalDate;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class StudyControllerIT extends IntegrationTest {

  private static final String STUDY_PATH = "/study";
  @Autowired public MockMvc mockMvc;
  UserDetails user1;
  UserDetails user2;
  UserDetails user3;
  @Autowired private ObjectMapper mapper;
  @Autowired private StudyRepository studyRepository;
  @Autowired private UserDetailsRepository userDetailsRepository;

  @Before
  public void setupStudies() {
    studyRepository.deleteAll();
    user1 = UserDetails.builder().userId("user1").approved(true).build();
    userDetailsRepository.save(user1);
    user2 = UserDetails.builder().userId("user2").approved(true).build();
    userDetailsRepository.save(user2);
    Study draftStudy =
        Study.builder()
            .name("draft")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.DRAFT)
            .build();
    studyRepository.save(draftStudy);
    Study pendingStudy =
        Study.builder()
            .name("pending")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.PENDING)
            .build();
    studyRepository.save(pendingStudy);
    Study reviewingStudy =
        Study.builder()
            .name("reviewing")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.REVIEWING)
            .build();
    studyRepository.save(reviewingStudy);
    Study changeRegStudy =
        Study.builder()
            .name("changeRequest")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.CHANGE_REQUEST)
            .build();
    studyRepository.save(changeRegStudy);
    Study deniedStudy =
        Study.builder()
            .name("denied")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.DENIED)
            .build();
    studyRepository.save(deniedStudy);
    Study approvedStudy =
        Study.builder()
            .name("approved")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.APPROVED)
            .build();
    studyRepository.save(approvedStudy);
    Study publishedStudy =
        Study.builder()
            .name("published")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.PUBLISHED)
            .build();
    studyRepository.save(publishedStudy);
    Study closedStudy =
        Study.builder()
            .name("closed")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(StudyStatus.CLOSED)
            .build();
    studyRepository.save(closedStudy);
  }

  @After
  public void clearStudies() {
    studyRepository.deleteAll();
    userDetailsRepository.deleteById("user1");
    userDetailsRepository.deleteById("user2");
  }

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
        StudyDto.builder()
            .name("s1")
            .goal("Default")
            .endDate(LocalDate.now())
            .startDate(LocalDate.now())
            .firstHypotheses("fh1")
            .status(StudyStatus.DRAFT)
            .build();
    String studyJson = mapper.writeValueAsString(validStudy);

    mockMvc
        .perform(
            post(STUDY_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isForbidden());
  }

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
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

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user1")
  public void studyCoordinatorShouldGetAllHisStudies() {

    MvcResult result =
        mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    StudyDto[] studies =
        mapper.readValue(result.getResponse().getContentAsString(), StudyDto[].class);
    assertEquals(8, studies.length);
  }

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      userId = "user2")
  public void researcherInAStudyShouldGetPublicAndClosedStudies() {

    MvcResult result =
        mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    StudyDto[] studies =
        mapper.readValue(result.getResponse().getContentAsString(), StudyDto[].class);
    assertEquals(2, studies.length);
    assertNotNull(
        Arrays.stream(studies)
            .filter(studyDto -> studyDto.getName().equals("closed"))
            .findFirst()
            .orElse(null));
    assertNotNull(
        Arrays.stream(studies)
            .filter(studyDto -> studyDto.getName().equals("published"))
            .findFirst()
            .orElse(null));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      userId = "user1")
  public void researcherNotInAStudyShouldNotGetStudies() {

    MvcResult result =
        mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    StudyDto[] studies =
        mapper.readValue(result.getResponse().getContentAsString(), StudyDto[].class);
    assertEquals(0, studies.length);
  }

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_APPROVER},
      userId = "user1")
  public void studyApproverShouldGetPendingAndReviewingStudies() {

    MvcResult result =
        mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    StudyDto[] studies =
        mapper.readValue(result.getResponse().getContentAsString(), StudyDto[].class);
    assertEquals(2, studies.length);
    assertNotNull(
        Arrays.stream(studies)
            .filter(studyDto -> studyDto.getName().equals("pending"))
            .findFirst()
            .orElse(null));
    assertNotNull(
        Arrays.stream(studies)
            .filter(studyDto -> studyDto.getName().equals("reviewing"))
            .findFirst()
            .orElse(null));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {SUPER_ADMIN},
      userId = "user1")
  public void superAdminShouldNotGetStudies() {

    mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {ORGANIZATION_ADMIN},
      userId = "user1")
  public void orgAdminShouldNotBeAllowedToGetStudies() {

    mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user2")
  public void studyCoordinatorShouldNotGetOtherCoordinatorsStudies() {

    MvcResult result =
        mockMvc.perform(get(STUDY_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    StudyDto[] studies =
        mapper.readValue(result.getResponse().getContentAsString(), StudyDto[].class);
    assertEquals(0, studies.length);
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_APPROVER},
      userId = "user2")
  public void shouldOnlyAllowUpdatingStatusForApproverRole() {
    String studyName = "unchanged";

    Study unchangedStudy =
        Study.builder()
            .name(studyName)
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user1))
            .status(StudyStatus.REVIEWING)
            .build();
    Study study = studyRepository.save(unchangedStudy);

    StudyDto updateStudy =
        StudyDto.builder()
            .name("s1")
            .goal("goal")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(5))
            .firstHypotheses("fh1")
            .status(StudyStatus.APPROVED)
            .build();

    String studyJson = mapper.writeValueAsString(updateStudy);

    mockMvc
        .perform(
            put(String.format("%s/%s", STUDY_PATH, study.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(studyName))
        .andExpect(jsonPath("$.status").value(StudyStatus.APPROVED.name()));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user1")
  public void shouldUpdateStudySuccessfully() {
    Study createStudy =
        Study.builder()
            .name("createStudy")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user1))
            .build();
    Study study = studyRepository.save(createStudy);

    StudyDto updateStudy =
        StudyDto.builder()
            .name("updateStudy")
            .goal("goal")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(5))
            .firstHypotheses("fh1")
            .status(StudyStatus.PENDING)
            .build();

    String studyJson = mapper.writeValueAsString(updateStudy);

    mockMvc
        .perform(
            put(String.format("%s/%s", STUDY_PATH, study.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updateStudy"))
        .andExpect(jsonPath("$.status").value(StudyStatus.PENDING.name()));
  }
}
