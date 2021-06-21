package de.vitagroup.num.integrationtesting.tests;

import static de.vitagroup.num.domain.Roles.ORGANIZATION_ADMIN;
import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
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
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.repository.ProjectRepository;
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

public class ProjectControllerIT extends IntegrationTest {

  private static final String PROJECT_PATH = "/project";
  @Autowired public MockMvc mockMvc;
  UserDetails user1;
  UserDetails user2;
  UserDetails user3;
  @Autowired private ObjectMapper mapper;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserDetailsRepository userDetailsRepository;

  @Before
  public void setupProjects() {
    projectRepository.deleteAll();
    user1 = UserDetails.builder().userId("user1").approved(true).build();
    userDetailsRepository.save(user1);
    user2 = UserDetails.builder().userId("user2").approved(true).build();
    userDetailsRepository.save(user2);
    Project draftProject =
        Project.builder()
            .name("draft")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.DRAFT)
            .build();
    projectRepository.save(draftProject);
    Project pendingProject =
        Project.builder()
            .name("pending")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.PENDING)
            .build();
    projectRepository.save(pendingProject);
    Project reviewingProject =
        Project.builder()
            .name("reviewing")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.REVIEWING)
            .build();
    projectRepository.save(reviewingProject);
    Project changeRegProject =
        Project.builder()
            .name("changeRequest")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.CHANGE_REQUEST)
            .build();
    projectRepository.save(changeRegProject);
    Project deniedProject =
        Project.builder()
            .name("denied")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.DENIED)
            .build();
    projectRepository.save(deniedProject);
    Project approvedProject =
        Project.builder()
            .name("approved")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.APPROVED)
            .build();
    projectRepository.save(approvedProject);
    Project publishedProject =
        Project.builder()
            .name("published")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.PUBLISHED)
            .build();
    projectRepository.save(publishedProject);
    Project closedProject =
        Project.builder()
            .name("closed")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user2))
            .status(ProjectStatus.CLOSED)
            .build();
    projectRepository.save(closedProject);
  }

  @After
  public void clearProjects() {
    projectRepository.deleteAll();
    userDetailsRepository.deleteById("user1");
    userDetailsRepository.deleteById("user2");
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldValidateProject() {
    ProjectDto invalidProject = ProjectDto.builder().build();
    String studyJson = mapper.writeValueAsString(invalidProject);

    mockMvc
        .perform(
            post(PROJECT_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {RESEARCHER})
  public void shouldNotAccessProjectApiWithWrongRole() {
    ProjectDto validProject =
        ProjectDto.builder()
            .name("s1")
            .goal("Default")
            .endDate(LocalDate.now())
            .startDate(LocalDate.now())
            .firstHypotheses("fh1")
            .status(ProjectStatus.DRAFT)
            .build();
    String studyJson = mapper.writeValueAsString(validProject);

    mockMvc
        .perform(
            post(PROJECT_PATH)
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
  public void shouldCreateProject() {
    ProjectDto validProject =
        ProjectDto.builder().name("s1").firstHypotheses("fh1").status(ProjectStatus.DRAFT).build();
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

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user1")
  public void studyCoordinatorShouldGetAllHisProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);
    assertEquals(8, projects.length);
  }

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      userId = "user2")
  public void researcherInAStudyShouldGetPublicAndClosedProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);
    assertEquals(2, projects.length);
    assertNotNull(
        Arrays.stream(projects)
            .filter(projectDto -> projectDto.getName().equals("closed"))
            .findFirst()
            .orElse(null));
    assertNotNull(
        Arrays.stream(projects)
            .filter(projectDto -> projectDto.getName().equals("published"))
            .findFirst()
            .orElse(null));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      userId = "user1")
  public void researcherNotInAStudyShouldNotGetProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);
    assertEquals(0, projects.length);
  }

  @Ignore(
      "Ignore until integration testing infrastructure includes keycloak dependency as container")
  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_APPROVER},
      userId = "user1")
  public void studyApproverShouldGetPendingAndReviewingProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);
    assertEquals(2, projects.length);
    assertNotNull(
        Arrays.stream(projects)
            .filter(projectDto -> projectDto.getName().equals("pending"))
            .findFirst()
            .orElse(null));
    assertNotNull(
        Arrays.stream(projects)
            .filter(projectDto -> projectDto.getName().equals("reviewing"))
            .findFirst()
            .orElse(null));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {SUPER_ADMIN},
      userId = "user1")
  public void superAdminShouldNotGetProjects() {

    mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {ORGANIZATION_ADMIN},
      userId = "user1")
  public void orgAdminShouldNotBeAllowedToGetProjects() {

    mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user2")
  public void studyCoordinatorShouldNotGetOtherCoordinatorsProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH).with(csrf())).andExpect(status().isOk()).andReturn();
    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);
    assertEquals(0, projects.length);
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_APPROVER},
      userId = "user2")
  public void shouldOnlyAllowUpdatingStatusForApproverRole() {
    String name = "unchanged";

    Project unchangedProject =
        Project.builder()
            .name(name)
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user1))
            .status(ProjectStatus.REVIEWING)
            .build();
    Project project = projectRepository.save(unchangedProject);

    ProjectDto updateProject =
        ProjectDto.builder()
            .name("s1")
            .goal("goal")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(5))
            .firstHypotheses("fh1")
            .status(ProjectStatus.APPROVED)
            .build();

    String studyJson = mapper.writeValueAsString(updateProject);

    mockMvc
        .perform(
            put(String.format("%s/%s", PROJECT_PATH, project.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name))
        .andExpect(jsonPath("$.status").value(ProjectStatus.APPROVED.name()));
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user1")
  public void shouldUpdateProjectSuccessfully() {
    Project createProject =
        Project.builder()
            .name("createProject")
            .goal("Default")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now())
            .coordinator(user1)
            .researchers(Lists.newArrayList(user1))
            .build();
    Project project = projectRepository.save(createProject);

    ProjectDto updateStudy =
        ProjectDto.builder()
            .name("updateProject")
            .goal("goal")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(5))
            .firstHypotheses("fh1")
            .status(ProjectStatus.PENDING)
            .build();

    String projectJson = mapper.writeValueAsString(updateStudy);

    mockMvc
        .perform(
            put(String.format("%s/%s", PROJECT_PATH, project.getId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updateProject"))
        .andExpect(jsonPath("$.status").value(ProjectStatus.PENDING.name()));
  }
}