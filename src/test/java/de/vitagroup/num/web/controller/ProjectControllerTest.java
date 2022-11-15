package de.vitagroup.num.web.controller;

import static de.vitagroup.num.domain.Roles.ORGANIZATION_ADMIN;
import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import de.vitagroup.num.mapper.CommentMapper;
import de.vitagroup.num.mapper.ProjectMapper;
import de.vitagroup.num.mapper.ProjectViewMapper;
import de.vitagroup.num.service.CommentService;
import de.vitagroup.num.service.ProjectService;
import de.vitagroup.num.service.ehrbase.Pseudonymity;
import java.time.LocalDate;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("it")
@WebMvcTest(controllers = ProjectController.class)
public class ProjectControllerTest {
  private static final String PROJECT_PATH = "/project";
  @Autowired
  private ObjectMapper mapper;
  private MockMvc mockMvc;

  @MockBean
  private ProjectService projectService;

  @MockBean
  private CommentService commentService;

  @MockBean
  private ProjectMapper projectMapper;

  @MockBean
  private CommentMapper commentMapper;

  @MockBean
  private Pseudonymity pseudonymity;

  @MockBean
  private ProjectViewMapper projectViewMapper;

  @MockBean
  private ProjectRepository projectRepository;
  UserDetails user1;

  UserDetails user2;

  UserDetails user3;

  @BeforeEach
  void setUp(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders
        .webAppContextSetup(wac)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .alwaysDo(MockMvcResultHandlers.print())
        .build();
  }

  @Test
  @SneakyThrows
  public void shouldValidateProject() {
    ProjectDto invalidProject = ProjectDto.builder().build();
    String studyJson = mapper.writeValueAsString(invalidProject);

    mockMvc
        .perform(
            post(PROJECT_PATH)
                .with(csrf())
                .with(jwt()
                    .authorities(new SimpleGrantedAuthority("ROLE_" + RESEARCHER))
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
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
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + RESEARCHER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  public void researcherNotInAStudyShouldNotGetProjects() {

    MvcResult result =
        mockMvc.perform(
                get(PROJECT_PATH)
                    .with(csrf())
                    .with(jwt()
                        .jwt(builder -> builder.claim("sub", "user1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_" + RESEARCHER)))
            )
            .andExpect(status().isOk()).andReturn();

    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);

    assertEquals(0, projects.length);
  }

  @Test
  @SneakyThrows
  public void superAdminShouldNotGetProjects() {

    mockMvc.perform(
        get(PROJECT_PATH)
            .with(csrf())
            .with(jwt()
                .jwt(builder -> builder.claim("sub", "user1"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + SUPER_ADMIN)))
    ).andExpect(status().is4xxClientError());
  }

  @Test
  @SneakyThrows
  public void orgAdminShouldNotBeAllowedToGetProjects() {
    mockMvc.perform(
        get(PROJECT_PATH)
            .with(csrf())
            .with(jwt()
                .jwt(builder -> builder.claim("sub", "user1"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + ORGANIZATION_ADMIN)))
    ).andExpect(status().is4xxClientError());
  }

  @Disabled
  @Test
  @SneakyThrows
  public void studyCoordinatorShouldNotGetOtherCoordinatorsProjectsExceptProperStatuses() {

    MvcResult result = mockMvc.perform(
        get(PROJECT_PATH)
            .with(csrf())
            .with(jwt()
                .jwt(builder -> builder.claim("sub", "user2"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + STUDY_COORDINATOR)))
    ).andExpect(status().isOk())
        .andReturn();

    ProjectDto[] projects =
        mapper.readValue(result.getResponse().getContentAsString(), ProjectDto[].class);

    assertEquals(3, projects.length);
  }

  @Disabled
  @Test
  @SneakyThrows
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
                .with(jwt()
                    .jwt(builder -> builder.claim("sub", "user2"))
                    .authorities(new SimpleGrantedAuthority("ROLE_" + STUDY_APPROVER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(studyJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name))
        .andExpect(jsonPath("$.status").value(ProjectStatus.APPROVED.name()));
  }

  @Disabled
  @Test
  @SneakyThrows
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
                .with(jwt()
                    .jwt(builder -> builder.claim("sub", "user1"))
                    .authorities(new SimpleGrantedAuthority("ROLE_" + STUDY_COORDINATOR)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updateProject"))
        .andExpect(jsonPath("$.status").value(ProjectStatus.PENDING.name()));
  }
}
