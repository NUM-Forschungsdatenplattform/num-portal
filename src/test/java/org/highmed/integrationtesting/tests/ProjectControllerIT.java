package org.highmed.integrationtesting.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.highmed.integrationtesting.security.WithMockNumUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.highmed.domain.dto.ProjectDto;
import org.highmed.domain.dto.ProjectViewTO;
import org.highmed.domain.dto.TemplateInfoDto;
import org.highmed.domain.model.Organization;
import org.highmed.domain.model.Project;
import org.highmed.domain.model.ProjectCategories;
import org.highmed.domain.model.ProjectStatus;
import org.highmed.domain.model.admin.UserDetails;
import org.highmed.domain.repository.OrganizationRepository;
import org.highmed.domain.repository.ProjectRepository;
import org.highmed.domain.repository.UserDetailsRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.highmed.domain.model.Roles.*;

public class ProjectControllerIT extends IntegrationTest {

  private static final String PROJECT_PATH = "/project";
  @Autowired public MockMvc mockMvc;
  UserDetails user1;
  UserDetails user2;
  UserDetails user3;
  private ObjectMapper mapper = new ObjectMapper()
          .registerModule(new PageJacksonModule())
          .registerModule(new SortJacksonModule())
          .registerModule(new JavaTimeModule());
  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserDetailsRepository userDetailsRepository;
  @Autowired
  private OrganizationRepository organizationRepository;

  @Before
  public void setupProjects() {
    projectRepository.deleteAll();
    Optional<Organization> organizationOne = organizationRepository.findByName("Organization A");
    user1 = UserDetails.builder().userId("user1").approved(true).organization(organizationOne.get()).build();
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

  @Test
  @SneakyThrows
  @WithMockNumUser(roles = {STUDY_COORDINATOR})
  public void shouldCreateProject() {
    TemplateInfoDto templateInfoDto = TemplateInfoDto.builder()
            .templateId("Alter")
            .name("Alter")
            .build();
    ProjectDto validProject =
        ProjectDto.builder()
                .name("s1")
                .description("some dummy description")
                .firstHypotheses("fh1")
                .goal("demo project")
                .categories(Set.of(ProjectCategories.PREVENTION))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(50))
                .templates(List.of(templateInfoDto))
                .status(ProjectStatus.DRAFT).build();
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

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_COORDINATOR},
      userId = "user1")
  public void studyCoordinatorShouldGetAllHisProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH + "/all").with(csrf())).andExpect(status().isOk()).andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(8, projectsPage.getContent().size());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
          roles = {STUDY_COORDINATOR},
          userId = "user1")
  public void studyCoordinatorShouldGetAllHisProjectsPaginated() {
    MvcResult result =
            mockMvc.perform(get(PROJECT_PATH + "/all")
                            .param("page","0")
                            .param("size", "10")
                            .param("sort", "ASC")
                            .param("sortBy", "name")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(8, projectsPage.getContent().size());
    List<ProjectViewTO> projects = projectsPage.getContent();
    assertEquals("approved", projects.get(0).getName());

  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      userId = "user2")
  public void researcherInAStudyShouldGetPublishedProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH + "/all")
                .param("page","0")
                .param("size", "10")
                .with(csrf())).andExpect(status().isOk()).andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(1, projectsPage.getContent().size());
    assertNotNull(
            projectsPage.getContent().stream()
            .filter(projectViewTO -> projectViewTO.getName().equals("published"))
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
            mockMvc.perform(get(PROJECT_PATH + "/all").with(csrf())).andExpect(status().isOk()).andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(0, projectsPage.getContent().size());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {STUDY_APPROVER},
      userId = "user1")
  public void studyApproverShouldGetAllExceptDraftAndArchivedProjects() {

    MvcResult result =
        mockMvc.perform(get(PROJECT_PATH + "/all").with(csrf())).andExpect(status().isOk()).andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(7, projectsPage.getContent().size());
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("pending"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("reviewing"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("approved"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("changeRequest"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("denied"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("closed"))
            .findFirst()
            .orElse(null));
    assertNotNull(projectsPage.getContent().stream()
            .filter(projectDto -> projectDto.getName().equals("published"))
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

    mockMvc.perform(get(PROJECT_PATH + "/all").with(csrf())).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
          roles = {STUDY_COORDINATOR},
          userId = "user2")
  public void studyCoordinatorShouldNotGetOtherCoordinatorsProjectsExceptProperStatuses() {

    MvcResult result =
            mockMvc.perform(get(PROJECT_PATH + "/all").with(csrf())).andExpect(status().isOk()).andReturn();
    Page<ProjectViewTO> projectsPage = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertEquals(3, projectsPage.getContent().size());
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
